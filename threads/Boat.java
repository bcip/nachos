//yhdxt`oi`offt`of{inofinofmhphofx`ofxholhofuh`ov`ofphorih
//PART OF THE NACHOS. DON'T CHANGE CODE OF THIS LINE
package nachos.threads;

import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;
	
	private final static int Oahu = 0;
	private final static int Molokai = 1;
	
	private static int[] numOfChild = {0, 0};
	private static int[] numOfAdult = {0, 0};
	
	private static int numOfChildBoat;
	private static int locationOfBoat;
	
	private static Lock boat;
	private static Condition waitOnOahu;
	private static Condition waitBoarding;
	private static Condition waitOnMolokai;
	
	private static Communicator message;
	

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		// begin(3, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		
		numOfChild[0] = 0;
		numOfChild[1] = 0;
		
		numOfAdult[0] = 0;
		numOfAdult[1] = 0;
		
		locationOfBoat = Oahu;
		numOfChildBoat = 0;
		
		boat = new Lock();
		waitOnOahu = new Condition(boat);
		waitOnMolokai = new Condition(boat);
		waitBoarding = new Condition(boat);
		message = new Communicator();

		Runnable r = new Runnable() {
			public void run() {
				AdultItinerary();
			}
		};
		
		for(int i = 0; i < adults; i++){
			KThread t = new KThread(r);
			t.setName("Adult "+i);
			t.fork();
		}
		
		r = new Runnable() {
			public void run() {
				ChildItinerary();
			}
		};
		
		for(int i = 0; i < children; i++){
			KThread t = new KThread(r);
			t.setName("Child "+i);
			t.fork();
		}
		
		int total = children + adults;
		
		while(total != message.listen());

	}

	static void AdultItinerary() {
		bg.initializeAdult(); // Required for autograder interface. Must be the
								// first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.

		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */
		
		numOfAdult[Oahu]++;
		boat.acquire();
		
		while(locationOfBoat != Oahu || numOfChild[Oahu] > 1 || numOfChildBoat > 0){
			if (locationOfBoat == Oahu){
				waitOnOahu.wakeAll();
			}
			waitOnOahu.sleep();
		}
		
		bg.AdultRowToMolokai();
		numOfAdult[Oahu]--;
		numOfAdult[Molokai]++;
		locationOfBoat = Molokai;
		message.speak(numOfAdult[Molokai] + numOfChild[Molokai]);
		waitOnMolokai.wakeAll();
		boat.release();
	}

	static void ChildItinerary() {
		bg.initializeChild(); // Required for autograder interface. Must be the
								// first thing called.
		// DO NOT PUT ANYTHING ABOVE THIS LINE.
		
		int location = Oahu;
		numOfChild[Oahu]++;
		
		while(true){
			boat.acquire();
			if(location == Oahu){
				while(locationOfBoat != Oahu || numOfChildBoat == 2 || numOfChild[Oahu] == 1 ){
					if(locationOfBoat == Oahu){
						waitOnOahu.wakeAll();
					}
					waitOnOahu.sleep();
				}
				numOfChildBoat++;
				if(numOfChildBoat == 1){
					waitOnOahu.wakeAll();
					waitBoarding.sleep();
					
					numOfChild[Oahu]--;
					bg.ChildRideToMolokai();
					locationOfBoat = Molokai;
					location = Molokai;
					numOfChildBoat = 0;
					numOfChild[Molokai]++;
					message.speak(numOfAdult[Molokai] + numOfChild[Molokai]);
					
					waitOnMolokai.wakeAll();
					waitOnMolokai.sleep();
				}else{
					waitBoarding.wake();
					numOfChild[Oahu]--;
					location = Molokai;
					bg.ChildRowToMolokai();
					numOfChild[Molokai]++;
					waitOnMolokai.sleep();
				}
			}else{
				while(locationOfBoat != Molokai){
					waitOnMolokai.sleep();
				}
				
				numOfChild[Molokai]--;
				bg.ChildRowToOahu();
				location = Oahu;
				locationOfBoat = Oahu;
				numOfChild[Oahu]++;
				waitOnOahu.wakeAll();
				waitOnOahu.sleep();
			}
			boat.release();
		}
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out
				.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

}
