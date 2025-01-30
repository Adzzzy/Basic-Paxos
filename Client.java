import java.io.IOException;

public class Client {
	
	//main which will run Paxos by creating Elections
	public static void main(String[] args) throws IOException, InterruptedException {
		//there are 9 possible members (can differ for different test cases but will always be of these 9)
		Member M1, M2, M3, M4, M5, M6, M7, M8, M9;
		
		//create members to have an election with
		//The member ids given should be greater than 0 and be consecutive values so the ports may configure correctly
		//member ids 1-3 are proposers and each have their own delay peculiarities. Numbers greater than that are just acceptors.
		M1 = new Member(1);
		M2 = new Member(2);
		M3 = new Member(3);
		M4 = new Member(4);
		M5 = new Member(5);
		M6 = new Member(6);
		M7 = new Member(7);
		M8 = new Member(8);
		M9 = new Member(9);
		
		//create an election which has all members
		Election E1 = new Election(new Member[] {M1,M2,M3,M4,M5,M6,M7,M8,M9});
		
		Member electedMember = E1.holdElection();
		
		int memberId = electedMember.memberNum;
		
		//sleep for several seconds to allow other threads to finish before printing the winner and closing the acceptor sockets
		Thread.sleep(7000);
		
		System.out.println("\nElection Winner is M" + memberId);
		
		//have the election close the server sockets of its acceptors
		E1.closeListeners();
		
		/*
		//create an election which has only three members
		
		//create new members so they don't bring over any fields from the old election
		M1 = new Member(1);
		M2 = new Member(2);
		M3 = new Member(3);
		Election E2 = new Election(new Member[] {M1,M2,M3});
		
		Member electedMember2 = E2.holdElection();
		
		int memberId2 = electedMember2.memberNum;
		
		//sleep for several seconds to allow other threads to finish before printing the winner and closing the acceptor sockets
		Thread.sleep(7000);
		
		System.out.println("\nElection Winner is M" + memberId2);
		
		//have the election close the server sockets of its acceptors
		E2.closeListeners();
		*/
		
		return;
	}

}
