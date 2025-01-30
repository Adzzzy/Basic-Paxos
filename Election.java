import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;

public class Election {
	//set to true when consensus has been reached to finish the election
	boolean consensus;
	
	int memberCount;
	//int proposerCount;
	int majority;
	Member[] MemberArray;
	
	//the amount of milliseconds considered small and large delays in the election
	int smallDelay;
	int largeDelay;
	
	public Election(Member[] Members) {
		consensus = false;
		
		MemberArray = Members;
		memberCount = MemberArray.length;
		
		//set delays
		smallDelay = 2000;
		largeDelay = 5000;
		
		//proposerCount = 0;
		
		/*
		//for loop to check through array and count proposers
		for (int i = 0; i < MemberArray.length; i++) {
			//M1, M2, and M3 are always proposers
			if (MemberArray[i].memberNum == 1 || MemberArray[i].memberNum == 2 || MemberArray[i].memberNum == 3) {
				proposerCount++;
			}
		}*/
		
		//majority is half + 1 (floor will ensure division on odd numbers will be rounded down first before adding the 1)
		majority = (int) Math.floor(memberCount/2 + 1);
	}
	
	//this method is used by the proposer to complete phase one of paxos (Send a Prepare message and handle the response (Promise or NAK))
	public boolean phaseOne (Member proposer) {
		//generate unique id for the prepare message by using the current timestamp combined with a hash "#" followed by the proposer's member number
		String prepareID = Instant.now().toString() + "#" + proposer.memberNum;
		//set as the identifier the proposer is currently trying to create consensus on
		proposer.currentIdentifier = prepareID;
		
		//this will store the returned piggybacked values
		ArrayList<String> values = new ArrayList<String>();
		
		//threads will be created so that the proposer may send to each acceptor at the same time (all members are acceptors so memberCount is the amount of threads)
		for (int i = 1; i <= memberCount; i++) {
			//compiler needs to know that i will always be the same within the respective threads, store i in j so that it may be used within the thread scope
			int j = i;
			//thread created
			Thread t = new Thread() {
				public void run() {
					Socket s = null;
					PrintWriter out = null;
					BufferedReader in = null;
					try {
						//if consensus is reached and a thread reaches this code, it shouldn't try to send to the acceptors as they will close. Also members that are away (camping shouldn't run this code)
						if (!consensus && !proposer.away) {
							//create a socket to connect to Acceptor (will use localhost in this case)
							//connect to each acceptor's server socket so this proposer may exchange messages with the acceptors
							//the server socket will accept the connection and assign a new socket with a random port
							s = new Socket("localhost", 4567+j);
							
							out = new PrintWriter(s.getOutputStream());
							
							in = new BufferedReader(new InputStreamReader(s.getInputStream()));
							
							//print for testing
							System.out.println("M" + proposer.memberNum + ": " + "PREPARE " +  prepareID);
							//send a prepare message out (including the unique identifier)
							out.println("PREPARE " +  prepareID );
							out.flush();
							
							
							String response = in.readLine();
							
							//close the streams
							in.close();
							out.close();
							
							//if the response was empty then terminate the thread
							if (response == null) {
								return;
							}
							
							//if a promise was returned the attached value should be stored as non-null values need to be considered a candidate to propose when sending the accept message (the highest identifier's value should be chosen)
							//it's important to receive a majority of promises (the size of the "values" ArrayList can be used to indicate the amount of promises returned)
							if (response.startsWith("PROMISE")) {
								
								String value = response.split(" ")[2];
								
								//each proposer has their own values arrayList but then split into several threads, only one thread should be able to access at a time to avoid race conditions where a push might get lost
								synchronized (proposer) {
									values.add(value);
								}
							}
							//in the case of a NAK or invalid message, nothing will happen. This will be apparent if there aren't enough promises at the end.
						}
					}
					catch (Exception e) {
						System.err.println(e.toString());
					}
					finally {
						if (s != null) {
							try {
								s.close();
							} catch (IOException e) {
								System.err.println(e.toString());
							}
						}
					}
					return;
				}
			};
			//starts the thread
			t.start();
		}
		
		//timeout before checking for piggyback values and then if a majority has been received (method will be called again if not a majority)
		try {
			Thread.sleep(7000);
		} catch (InterruptedException e) {
			System.err.println(e.toString());
		}
		
		int proposalCount = values.size();
		
		if (proposalCount > 0) {
			//Of the returned piggybacked values, if any aren't null, then the one with the highest identifier should overwrite the proposers proposal value
			String largest = values.get(0);
			for (int i = 1; i < proposalCount; i++) {
				//if the values have only been null so far, set the current one as largest
				if (largest == null || largest.equals("null")) {
					largest = values.get(i);
				}
				//otherwise if the value at the index being looked at isn't null as well they need to be compared to find which one is larger
				else if (values.get(i) != null && !values.get(i).equals("null")) {
					//Extract the timestamps and tie breaks so the identifiers can be compared
					String id = values.get(i).split(",")[0];
					
					String[] idTokens = id.split("#");
					String time = idTokens[0];
					String tieBreak = idTokens[1];
					
					String largestID = largest.split(",")[0];
					
					String[] largestIdTokens = largestID.split("#");
					String largestTime = largestIdTokens[0];
					String largestTieBreak = largestIdTokens[1];
					
					//Either the timestamp is outright higher
					if (Instant.parse(time).compareTo(Instant.parse(largestTime)) > 0) {
						largest = values.get(i);
					}
					//or the timestamp may be equal
					else if (Instant.parse(time).compareTo(Instant.parse(largestTime)) == 0) {
						//if the timestamp is equal, we compare the tiebreak attached to the end. If it is greater than or equal to that previously promised, its message can be considered.
						if (Integer.parseInt(tieBreak) >= Integer.parseInt(largestTieBreak)) {
							largest = values.get(i);
						}
					}
				}
			}
			
			//if "largest" is not equal to null then there was a value and the one with the highest identifier was stored into "largest".
			if (largest != null && !largest.equals("null")) {
				//Therefore, split the value away from the identifier and make it the new proposalValue for this proposer.
				proposer.proposalValue = Integer.parseInt(largest.split(",")[1]);
			}
		}
		
		//CHECK SIZE OF THE ARRAYLIST TO SEE IF A MAJORITY OF PROMISES WAS RECEIVED
		boolean majorityPromised = false;
		if (values.size() >= majority) {
			majorityPromised = true;
		}
		
		return majorityPromised;
	}
	
	//this method is used by the proposer to complete phase two of paxos (Send an Accept message and handle the response (Accepted or NAK))
	public boolean phaseTwo (Member proposer) {
		//this will be the ID used for the
		String acceptID = proposer.currentIdentifier;
		
		//The proposal value will be either the default preferred value (M1, M2, and M3 propose for themselves)
		//or will be the value associated with the max identifier returned by promises in phase one (if any of them weren't null)
		String proposeVal = Integer.toString(proposer.proposalValue);
		
		//the acceptedList is stored for this wave of accept messages, as an indicator for the proposer
		//Though, ultimately, the election itself will check each member to find if a majority have accepted the same identifier
		ArrayList<String> acceptedList = new ArrayList<String>();
		
		//threads will be created so that the proposer may send to each acceptor at the same time (all members are acceptors so memberCount is the amount of threads)
		for (int i = 1; i <= memberCount; i++) {
			//compiler needs to know that i will always be the same within the respective threads, store i in j so that it may be used within the thread scope
			int j = i;
			//thread created
			Thread t = new Thread() {
				public void run() {
					Socket s = null;
					try {
						//if consensus is reached and a thread reaches this code, it shouldn't try to send to the acceptors as they will close
						if (!consensus && !proposer.away) {
							//create a socket to connect to Acceptor (will use localhost in this case)
							//connect to each acceptor's server socket so this proposer may exchange messages with the acceptors
							//the server socket will accept the connection and assign a new socket with a random port
							s = new Socket("localhost", 4567+j);
							
							PrintWriter out = new PrintWriter(s.getOutputStream());
							
							BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
							
							//print for testing
							System.out.println("M" + proposer.memberNum + ": " + "ACCEPT " +  acceptID + " " + proposeVal);
							//send a prepare message out (including the unique identifier)
							out.println("ACCEPT " +  acceptID + " " + proposeVal);
							out.flush();
							
							String response = in.readLine();
							
							//close the streams
							in.close();
							out.close();
							
							//if the response was empty then terminate the thread
							if (response == null) {
								return;
							}
							
							//accepted messages means the acceptor has stored the identifier and value on their side. Here we just need to know if a resend might be necessary.
							if (response.startsWith("ACCEPTED")) {
								//each proposer has their own acceptedList but accessed by several threads, only one thread should be able to access at a time to avoid race conditions where a push might get lost
								synchronized (proposer) {
									acceptedList.add(response);
								}
							}
							//in the case of a NAK or invalid message, nothing will happen. This will be apparent by the acceptedCount at the end, and will cause the process to repeat if a majority isn't attained.
						}
					}
					catch (Exception e) {
						System.err.println(e.toString());
					}
					finally {
						if (s != null) {
							try {
								s.close();
							} catch (IOException e) {
								System.err.println(e.toString());
							}
						}
					}
					return;
				}
			};
			//starts the thread
			t.start();
		}
		
		//timeout before checking if a majority has been received (method will be called again if not a majority)
		try {
			Thread.sleep(7000);
		} catch (InterruptedException e) {
			System.err.println(e.toString());
		}
		
		//check amount of accepted messages to see if consensus was reached in this wave of accept messages alone
		boolean majorityAccepted = false;
		if (acceptedList.size() >= majority) {
			majorityAccepted = true;
		}
		return majorityAccepted;
	}
	
	//each proposer runs this method until consensus is reached (arg is the member who is proposing)
	public void propose(Member proposer) {
		//new thread is created for the proposer to use in this method so the election can continue on to other members
		Thread thread = new Thread() {
			//this is the threaded function
			public void run() {
				//continue looping while there isn't consensus.
				while (!consensus) {
					//run the first phase (sending Prepare and handling Promise or NAK) until a majority of promises are received
					while (!phaseOne(proposer)) {
						//can put a timeout (sleep) here if need be.
					}
					
					//Allow chance to break before running phase 2 if consensus has been reached
					if (consensus) {
						break;
					}
					//next is phase two (sending Accept and handling Accepted or NAK)
					phaseTwo(proposer);
					
					//after doing phase 2, if the election determines there to be a consensus, the algorithm with have concluded
					//otherwise the loop will keep running until consensus is achieved
				}
				return;
			}
		};
		thread.start();
		return;
	}
	
	//each acceptor runs this method until consensus is reached (arg is the member who is accepting)
	public void accept(Member acceptor) {
		//new thread is created for the acceptor so the election can continue on to other members
		Thread thread = new Thread() {
			//this is the threaded function
			public void run() {
				//initialise socket here so that it may be referenced after the while loop finishes to close the socket
				Socket s = null;
				//acceptor listens for connections while making handler threads to send back promises, accepted messages, etc. 
				//Will eventually stop when consensus has been reached on a value (consensus variable is stored in the election)
				while (!consensus) {
					try {
						//blocks here, accepting incoming connections from proposers
						s = acceptor.serverS.accept();
						
						//let the acceptors break when consensus is reached and sockets are closed, instead of attempting to continue with the closed sockets
						if (consensus) {
							break;
						}
						
						PrintWriter out = new PrintWriter(s.getOutputStream());
						
						BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
						
						//create another thread now that represents (and handles) a connection between this acceptor and a connected proposer
						Thread t = new Thread() {
							public void run() {
								//read in and parse the received message
								String message = null;
								
								try {
									message = in.readLine();
								} catch (IOException e) {
									//if the message read fails, close the streams and return
									if (in != null && out !=null) {
										try {
											in.close();
											out.close();
										} catch (IOException e1) {
											System.err.println(e.toString());
										}
									}
									//return and end thread if there was a problem reading in from the socket
									return;
								}
								
								//split the message into tokens
								String[] tokens = message.split(" ");
								String mType = tokens[0];
								String id = tokens[1];
								
								//boolean for the possibility of noResponse
								boolean noResponse = false;
								
								//generate a random int between 1 and 10 to determine the time delay
								int randInt = (new Random()).nextInt(10)+1;
								
								//M3 is camping sometimes which prevents responding
								//M3 has a 1 in 5 chance of coming back from camping upon receiving a message
								int randInt5 = (new Random()).nextInt(5)+1;
								
								//M3 has a 1 in 20 chance of going camping upon receiving a message
								int randInt20 = (new Random()).nextInt(20)+1;
								
								if (acceptor.memberNum == 3) {
									//if randInt5 is a 1 (out of 5 possible values)
									if (randInt5 == 1 && acceptor.away) {
										//return from camping
										acceptor.away = false;
										System.out.println("M" + acceptor.memberNum + ": Returned from camping");
									}
									//else if randInt20 is a 1 (out of 20 possible values)
									else if (randInt20 == 1 && !acceptor.away) {
										acceptor.away = true;
										System.out.println("M" + acceptor.memberNum + ": Went camping");
									}
								}
								
								if (acceptor.away) {
									noResponse = true;
								}
								
								//different members have different possible response delays to received messages
								//delayMsg will be attached to the terminal printouts to indicate delay (empty string means immediate response)
								String delayMsg = "";
								
								//M1 always responds immediately so no delay (other than the transmission delay of the sockets)
								
								//M2 usually is in the hills so has large delay mostly, but sometimes in a cafe and has immediate response
								if (acceptor.memberNum == 2) {
									//70% chance of being in the hills
									if (randInt <= 7) {
										//Firstly, when M2 is in the hills they don't usually respond to all emails
										int randInt2 = (new Random()).nextInt(10)+1;
										//50% chance of not responding to a given message
										if (randInt2 > 5) {
											noResponse = true;
										}
										//otherwise if there is a response, there will be a large delay
										else {
											try {
												Thread.sleep(largeDelay);
												delayMsg = " (large delay)";
											} catch (InterruptedException e) {
												System.err.println(e.toString());
											}
										}
									} //otherwise in the cafe so no delay (immediate response)
								}
								
								//M3 always has a small delay (unless they're away camping in which case there will be no response)
								else if (acceptor.memberNum == 3 && !acceptor.away) {
									try {
										Thread.sleep(smallDelay);
										delayMsg = " (small delay)";
									} catch (InterruptedException e) {
										System.err.println(e.toString());
									}
								}
								
								//All other members vary in delays and have a chance of all 3
								else if (acceptor.memberNum > 3) {
									//a random int of 3 to 8 will result in small delay (60% chance)
									if (randInt >= 3 && randInt <= 8) {
										try {
											Thread.sleep(smallDelay);
											delayMsg = " (small delay)";
										} catch (InterruptedException e) {
											System.err.println(e.toString());
										}
									}
									//9 or 10 will result in a large delay (20%)
									else if (randInt >= 9) {
										try {
											Thread.sleep(largeDelay);
											delayMsg = " (large delay)";
										} catch (InterruptedException e) {
											System.err.println(e.toString());
										}
									}
									//otherwise a roll of 1 or 2 will result in immediate response
								}
								
								//if the acceptor won't respond, print that they didn't respond
								if (noResponse) {
									//
									System.out.println("M" + acceptor.memberNum + ": Didn't respond to M" + id.split("#")[1] + "'s " + mType + " message");
								}
								//otherwise the message will be handled accordingly
								else {
									//check that the id received is greater than or equal to that which has already been promised to
									boolean higher = false;
									
									//need a synchronized block since accessing shared variables (maxPromised and acceptedMember) can create race conditions
									//synchronizes on the acceptor meaning that several different acceptors can still run the thread at a time just not the same acceptor
									synchronized (acceptor) {
										//split the tie break off of the identifier
										String[] idTokens = id.split("#");
										String time = idTokens[0];
										String tieBreak = idTokens[1];
										
										//if we have a previously promised identifier, it'll need to be compared
										if (acceptor.maxPromised != null) {
											String[] maxIdTokens = acceptor.maxPromised.split("#");
											String maxTime = maxIdTokens[0];
											String maxTieBreak = maxIdTokens[1];
											
											//Either the timestamp is outright higher
											if (Instant.parse(time).compareTo(Instant.parse(maxTime)) > 0) {
												higher = true;
											}
											//or the timestamp may be equal
											else if (Instant.parse(time).compareTo(Instant.parse(maxTime)) == 0) {
												//if the timestamp is equal, we compare the tiebreak attached to the end. If it is greater than or equal to that previously promised, its message can be considered.
												if (Integer.parseInt(tieBreak) >= Integer.parseInt(maxTieBreak)) {
													higher = true;
												}
											}
										}
										//if it's not equal to null, there have been no other promises so it's the highest identifier seen so far
										else {
											higher = true;
										}
										
										//have if statements for prepare messages, accept messages, and else to handle unexpected messages
										if (mType.equals("PREPARE")) {
											//PREPARE has been received and has id that is equal or greater than that which it may promise itself to, so set the new promised ID
											if (higher) {
												acceptor.maxPromised = id;
												//promise needs to have any previously accepted proposal sent back (or null if there are none)
												String accepted = "null";
												//if there was something accepted, send back the accepted identifier and value separated by a colon
												if (acceptor.acceptedMember[0] != null) {
													accepted = acceptor.acceptedMember[0] + "," + acceptor.acceptedMember[1];
												}
												//print to terminal for debugging purposes
												System.out.println("M" + acceptor.memberNum + delayMsg + ": " + "PROMISE " + id + " " + accepted);
												//return a promise message with the id and the previously accepted member (will be null if nothing has been accepted yet)
												out.println("PROMISE " + id + " " + accepted);
												out.flush();
											}
											else {
												//print to terminal for debugging purposes
												System.out.println("M" + acceptor.memberNum + delayMsg +  ": " + "NAK " + id);
												out.println("NAK " + id);
												out.flush();
											}
										}
										else if (mType.equals("ACCEPT")) {
											//accept message should have a value as well
											String value = tokens[2];
											//if the Accept message doesn't have a lower id than that already promised to, send back an accepted message
											if (higher) {
												acceptor.maxPromised = id;
												//Accept the value and store it (it may be piggybacked in future promise messages)
												acceptor.acceptedMember[0] = id;
												acceptor.acceptedMember[1] = value;
												//print to terminal for debugging purposes
												System.out.println("M" + acceptor.memberNum + delayMsg +  ": " + "ACCEPTED " + id + " " + value);
												out.println("ACCEPTED " + id + " " + value);
												out.flush();
											}
											//else the acceptor is promised to a higher id so a NAK is sent back
											else {
												//print to terminal for debugging purposes
												System.out.println("M" + acceptor.memberNum + delayMsg + ": " + "NAK " + id);
												out.println("NAK " + id);
												out.flush();
											}
										}
										//else message isn't a prepare or accept message, so send back a NAK
										else {
											System.out.println("M" + acceptor.memberNum + delayMsg + ": " + "NAK " + id);
											out.println("NAK " + id);
											out.flush();
										}
									}
								}
								return;
								
							}
						};
						//starts the thread
						t.start();
						
					}
					//No error message because serverSocket will be closed while listening after consensus is reached
					catch (Exception e) {
						//System.out.println("M" + acceptor.memberNum + "'s server socket closed");
					}
				}
				if (s != null) {
					try {
						//sleep for several seconds to allow other threads to finish before closing sockets
						Thread.sleep(15000);
						s.close();
					} catch (IOException | InterruptedException e) {
						System.err.println(e.toString());
					}
				}
				return;
			}
		};
		thread.start();
		return;
	}
	
	//hold an election and return the member who wins the election
	public Member holdElection() {
		
		System.out.println("Election Start (" + MemberArray.length + " members)");
		
		//member elected via paxos, initially null
		Member electedMember = null;
		
		//Have each member run the accept function first so that they are ready to accept proposals
		for (int i = 0; i < MemberArray.length; i++) {
			//everyone is an acceptor
			accept(MemberArray[i]);
		}

		//now that acceptors are ready loop through to check for proposers and have them run propose
		for (int i = 0; i < MemberArray.length; i++) {
			//M1, M2, and M3 are always proposers
			if (MemberArray[i].memberNum == 1 || MemberArray[i].memberNum == 2 || MemberArray[i].memberNum == 3) {
				
				propose(MemberArray[i]);
			}
		}
		
		//loop until the election is done (check that a majority of members have reached consensus on a value (check acceptedMember variables aren't null, and majority have the same value))
		while (electedMember == null) {
			//this array list will keep track of how many times each identifier appears across members' accepted identifiers. A list of string arrays where [0] is the identifier and [1] is the appearances
			//if the identifiers appear majority or more times, consensus has been reached on that identifier and it's value will decide the elected member
			ArrayList<String[]> identifierTally = new ArrayList<String[]>();
			
			//look at the identifier each member has stored as accepted
			for (int i = 0; i < MemberArray.length; i++) {
				//the accepted identifier of the current member being looked at is stored in acceptedMember[0] (acceptedMember[1] holds the associated value)
				String identifier = MemberArray[i].acceptedMember[0];
				
				if (identifier != null) {
					//need to check if identifier already exists in the arrayList and if so, where it appears
					boolean identifierExists = false;
					int identifierIndex = 0;
					
					//check whether the identifier is already in identifierTally, and if so, increase it's appearance count
					for (int j = 0; j < identifierTally.size(); j++) {
						if (identifierTally.get(j)[0].equals(identifier)) {
							identifierExists = true;
							identifierIndex = j;
						}
					}
					
					//if the identifier already exists in identifierTally, then increase the appearance count by 1
					if (identifierExists) {
						int newCount = (Integer.parseInt(identifierTally.get(identifierIndex)[1])) + 1;
						String temp[] = {identifier,Integer.toString(newCount)};
						identifierTally.set(identifierIndex, temp);
					}
					//otherwise add the identifier along with it's appearances (1 appearance)
					else {
						String temp[] = {identifier,"1"};
						identifierTally.add(temp);
					}
				}
			}
			
			//The appearances of each identifier have been tallied up now, so check if any appear majority or more times
			//the identifier that has reached consensus if any and its associated value
			String identifier = null;
			String value = null;
			
			for (int i = 0; i < identifierTally.size(); i++) {
				//check if identifier appears a majority or more times
				if (Integer.parseInt(identifierTally.get(i)[1]) >= majority) {
					//that identifier appears majority or more times across the members so we have consensus
					consensus = true;
					identifier = identifierTally.get(i)[0];
					break;
				}
			}
			
			//if we have a consensus then we should find the value and the member that represents and return them as the elected member
			if (consensus) {
				//find the value associated with the identifier
				//check the members to find the identifier
				for (int j = 0; j < MemberArray.length; j++) {
					try {
						//found a member with the identifier so store the associated value
						if (MemberArray[j].acceptedMember[0].equals(identifier)) {
							value = MemberArray[j].acceptedMember[1];
							break;
						}
					}
					//may compare string to null so catch NullPointerException
					catch (NullPointerException e) {}
				}
				
				//the elected member is the one whose member id matches that value
				for (int j = 0; j < MemberArray.length; j++) {
					if (MemberArray[j].memberNum == Integer.parseInt(value)) {
						electedMember = MemberArray[j];
						break;
					}
				}
			}
		}
		
		return electedMember;
	}
	
	//method to close the server sockets of the acceptors in the election
	public void closeListeners() throws IOException, InterruptedException {
		
		for (int i = 0; i < MemberArray.length; i++) {
			MemberArray[i].serverS.close();
		}
		return;
	}

}
