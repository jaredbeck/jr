public class ShutdownInterceptor extends Thread {

	private IApp app;
	
	public ShutdownInterceptor(IApp app) {
		super("JR-ShutdownInterceptor"); // set Thread name
		this.app = app;
	}
	
	public void run() {
		System.out.println("DEBUG: ShutdownInterceptor: Call the shutdown routine");
		app.shutDown();
	}

}