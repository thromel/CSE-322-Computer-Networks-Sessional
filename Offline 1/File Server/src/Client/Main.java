package Client;

import java.util.Scanner;

public class Main {

	public static boolean isLoggedIn = false;

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Username: ");
		String userName = scanner.nextLine();

		Client client = new Client("localhost",8818, false, userName);
		Thread t = new Thread(client);
		t.start();



		while (true){
			String msg = scanner.nextLine();
			try{
				Thread.sleep(100);
			} catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}
