package abc_pipeline_engine.sysinfo.data;

public class SysInfo {
	// CPU空闲率
	private double freeCpu;
	// 空闲内存
	private long freeMem;

	public double getFreeCpu() {
		return freeCpu;
	}

	public void setFreeCpu(double freeCpu) {
		this.freeCpu = freeCpu;
	}

	public long getFreeMem() {
		return freeMem;
	}

	public void setFreeMem(long freeMem) {
		this.freeMem = freeMem;
	}


}
