package abc_pipeline_engine.execution.utils;

import base.operators.Process;


public class ProcessThread extends Thread {

	private Process process;

	public ProcessThread(final Process process) {
		super("ProcessThread");
		this.process = process;
	}

	public ProcessThread() {
		super("ProcessThread");
	}

	public void setProcess(Process process){
		this.process = process;
	}

	@Override
	public void run() {
		try {
			process.run();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (process.getProcessState() != Process.PROCESS_STATE_STOPPED) {
				process.stop();
			}
			this.process = null;
		}
	}


	public void stopProcess() {
		if (process != null) {
			System.out.println("进入stopProcess"+process);
			this.process.stop();

			this.stop();

		}
	}

	public void pauseProcess() {
		if (process != null) {
			this.process.pause();
		}
	}

	@Override
	public String toString() {
		return "ProcessThread (" + process.getProcessLocation() + ")";
	}


}
