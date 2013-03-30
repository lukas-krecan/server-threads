package net.javacrumbs.test;

import java.util.Random;

public class CreateUsefulThreads {
	/**
	 * Thread class
	 * 
	 * @author Lukas Krecan
	 * 
	 */
	private static final class MyThread extends Thread {
		private final int number;
		
		private static final Random random = new Random();

		public MyThread(int number) {
			this.number = number;
		}

		@Override
		public void run() {
			if (shouldPrintMessage(number))
			{
				System.out.println("Thread no. "+number+" started.");
			}
			try {
				while(true)
				{
					Thread.sleep(random.nextInt(120000));
					if (shouldPrintMessage(number))
					{
						System.out.println("Thread number "+number+" woke up.");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
   	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		final int noOfThreads = 32000;
		int i = 0;
		try {
			for (i = 0; i < noOfThreads; i++) {
				if (shouldPrintMessage(i))
				{
					System.out.println("Creating thread "+i+" ("+(System.currentTimeMillis()-startTime)+"ms)");
				}
				new MyThread(i).start();
			}
		} catch (Throwable e) {
			System.out.println("Error thrown when creating thread " + i);
			e.printStackTrace();
		}
	}

	private static boolean shouldPrintMessage(int i) {
		return i % 100 == 0;
	}
}
