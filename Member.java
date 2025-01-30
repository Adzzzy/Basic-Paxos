import java.io.IOException;
import java.net.*;

public class Member {
	//id of the member
	int memberNum;
	
	//The value the member wants to propose (will be null if not a proposer (only 1, 2, and 3 are proposers)) and can be overwritten by piggyback in promise
	int proposalValue;
	
	//a proposer member's current identifier which it uses in it's prepare and accept messages (will stay null for non-proposers)
	String currentIdentifier;
	
	//the max identifier the member has currently promised itself to
	String maxPromised;
	
	//if a value has been accepted, it is then propagated back and stored in this variable.
	//should contain both identifier and the value (the memberNum to represent the member that was accepted)
	String[] acceptedMember;
	
	//ServerSocket which the member will use to listen for new connections as an acceptor
	ServerSocket serverS;
	
	boolean away;
	
	public Member(int memberNum) throws IOException {
		this.memberNum = memberNum;
		
		currentIdentifier = null;
		
		//members 1, 2, and 3 want to propose themselves
		if (this.memberNum == 1 || this.memberNum == 2 || this.memberNum == 3) {
			proposalValue = this.memberNum;
		}
		//other members aren't proposers so just set to 0
		else {
			proposalValue = 0;
		}
		
		//null when the member is first created
		maxPromised = null;
		
		//null when the member is first created
		acceptedMember = new String[2];
		acceptedMember[0] = null;
		acceptedMember[1] = null;
		
		//this is for cases where a member is away for an extended period of time (such as when M3 is camping)
		away = false;
		
		//each member has their own ServerSocket listening on a different port
		//the server socket is used by the acceptors which the proposers connect to
		this.serverS = new ServerSocket(4567+memberNum);
	}
}
