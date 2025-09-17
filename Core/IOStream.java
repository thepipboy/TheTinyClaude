import java.io.*;

public class SimpleIOStream {
    public static void main(String[] args) {
        try {
            // Create a sample file
            FileWriter fw = new FileWriter("sample.txt");
            fw.write("Hello, World!\nThis is a Java IOStream example.");
            fw.close();
            
            // Read from file using FileInputStream
            FileInputStream fis = new FileInputStream("sample.txt");
            int data;
            System.out.print("File content: ");
            while ((data = fis.read()) != -1) {
                System.out.print((char) data);
            }
            fis.close();
            System.out.println();
            
            // Copy file using buffered streams
            BufferedInputStream bis = new BufferedInputStream(
                new FileInputStream("sample.txt"));
            BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream("copy.txt"));
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bis.close();
            bos.close();
            System.out.println("File copied successfully!");
            
            // Read copied file with FileReader
            FileReader fr = new FileReader("copy.txt");
            BufferedReader br = new BufferedReader(fr);
            System.out.print("Copied content: ");
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
            
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }
}