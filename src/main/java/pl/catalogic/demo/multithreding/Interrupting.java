package pl.catalogic.demo.multithreding;

public class Interrupting {

  public static void main(String[] args) {

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        System.out.println("Thread started");
      }
    });


    thread.setDaemon(true);
  }

}
