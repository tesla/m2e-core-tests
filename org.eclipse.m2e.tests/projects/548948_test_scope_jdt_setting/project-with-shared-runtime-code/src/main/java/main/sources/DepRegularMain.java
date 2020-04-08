package main.sources;

public class DepRegularMain {

	public static void main(String[] args)  {
		for (int i = 0; i < args.length; ++i) {
			System.out.format("DepRegularMain loading %1$30s: ", args[i]);
			try {
				ClassLoader.getSystemClassLoader().loadClass(args[i]);
				System.out.println("OK!");
			} catch (ClassNotFoundException e) {
				System.out.println("NOOOOOOOOOOOOOOO :-(");
			}
		}
	}
}
