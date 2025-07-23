package pl.catalogic.demo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import pl.catalogic.demo.s3.v2.S3_v2_service;

public class main {

  public static void main(String[] args) {
    var outputDir = new File("C:\\Projects\\demo-catalogic\\output");
    if (!outputDir.exists()) {
      if (outputDir.mkdirs()) {
        System.out.println("Створено папку: " + outputDir.getAbsolutePath());
      } else {
        System.err.println("Не вдалося створити папку!");
        return;
      }
    }

    for (int i = 1; i <= 500; i++) {
      var file = new File(outputDir, i + ".txt");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
        writer.write("3");
      } catch (IOException e) {
        System.err.println("Error writing file " + file.getAbsolutePath());
        e.printStackTrace();
      }
      if (i % 10_000 == 0) {
        System.out.println("Created files: " + i);
      }
    }
    System.out.println("DONE");
  }
}
