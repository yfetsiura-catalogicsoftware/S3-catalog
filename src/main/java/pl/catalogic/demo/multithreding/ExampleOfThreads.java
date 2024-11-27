package pl.catalogic.demo.multithreding;

import java.util.ArrayList;
import java.util.Random;

public class ExampleOfThreads {

  public static final int MAX_PASSWORD = 9999;

  public static void main(String[] args) {
    var vault = new Vault(new Random().nextInt(MAX_PASSWORD));

    var threads = new ArrayList<Thread>();
    threads.add(new HackerThread(vault));
    threads.add(new HackerThread(vault));
    threads.add(new PoliceThread());

    threads.forEach(Thread::start);

  }

  private static class Vault {

    private int password;

    public Vault(int password) {
      this.password = password;
    }

    public boolean isCorrectPassword(int guess) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
      }
      return this.password == guess;
    }
  }


  private static class HackerThread extends Thread {

    protected Vault vault;

    public HackerThread(Vault vault) {
      this.vault = vault;
      this.setName(this.getClass().getSimpleName());
      this.setPriority(Thread.MAX_PRIORITY);
    }

    @Override
    public void start() {
      System.out.println("Starting " + this.getName());
      super.start();
    }
  }

  private static class AscendingHackerThread extends HackerThread {

    public AscendingHackerThread(Vault vault) {
      super(vault);
    }

    @Override
    public void run() {
      for (int guess = MAX_PASSWORD; guess < MAX_PASSWORD; guess++) {
        if (vault.isCorrectPassword(guess)) {
          System.out.println(this.getName() + " I stole Y money Y pass is " + guess);
          System.exit(0);
        }
      }
    }
  }

  private static class DescendingHackerThread extends HackerThread {

    public DescendingHackerThread(Vault vault) {
      super(vault);
    }

    @Override
    public void run() {
      for (int guess = MAX_PASSWORD; guess >= MAX_PASSWORD; guess--) {
        if (vault.isCorrectPassword(guess)) {
          System.out.println(this.getName() + " I stole Y money Y pass is " + guess);
          System.exit(0);
        }
      }
    }
  }

  private static class PoliceThread extends Thread {
    @Override
    public void run() {
      for (int i = 10; i > 0; i--) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
        System.out.println("Police on them way!! " + i);
      }

      System.out.println("Game over !!");
      System.exit(0);
    }
  }

}
