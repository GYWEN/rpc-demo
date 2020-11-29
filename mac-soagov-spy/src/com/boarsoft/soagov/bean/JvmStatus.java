package com.boarsoft.soagov.bean;

import java.io.Serializable;

public class JvmStatus implements Serializable {
	private static final long serialVersionUID = 765411984771100386L;

	protected float ngcmn; // 新生代最小容量
	protected float ngcmx; // 新生代最大容量
	protected float ngc; // 当前新生代容量

	protected float s0c; // 第一个幸存区大小
	protected float s1c; // 第二个幸存区的大小
	protected float ec; // 伊甸园区的大小

	protected float ogcmn; // 老年代最小容量
	protected float ogcmx; // 老年代最大容量
	protected float ogc; // 当前老年代大小
	protected float oc; // 当前老年代大小

	protected float pgcmn; // Perm Gen
	protected float pgcmx;
	protected float pgc;
	protected float pc;

	protected float ygc; // 年轻代gc次数
	protected float fgc; // 老年代gc次数
	protected float fgct;
	protected float gct;

	protected float s0u; // Survivor space 0 Utilized
	protected float s1u; // Survivor space 1 Utilized
	protected float eu; // Eden Gen Utilized
	protected float ou; // Old Gen Utilized
	protected float pu; // Perm Gen Utilized

	protected float mcmn; // 最小元数据容量
	protected float mcmx; // 最大元数据容量
	protected float mc; // 当前元数据空间大小
	protected float mu; // 当前元数据空间已用大小

	protected float ccsmn; // 最小压缩类空间大小
	protected float ccsmx; // 最大压缩类空间大小
	protected float ccsc; // 当前压缩类空间大小

	// protected float sysCpuUsage;
	// protected float sysMemUsage;
	// protected float sysMemTotal;
	protected float appCpuUsage;
	
	protected int runnableThreads;
	protected int totalThreads;
	protected int classCount;
	protected float classSize;

	public int getClassCount() {
		return classCount;
	}

	public void setClassCount(int classCount) {
		this.classCount = classCount;
	}

	public float getClassSize() {
		return classSize;
	}

	public void setClassSize(float classSize) {
		this.classSize = classSize;
	}

	public int getRunnableThreads() {
		return runnableThreads;
	}

	public void setRunnableThreads(int runnableThreads) {
		this.runnableThreads = runnableThreads;
	}

	public int getTotalThreads() {
		return totalThreads;
	}

	public void setTotalThreads(int totalThreads) {
		this.totalThreads = totalThreads;
	}

	public float getAppCpuUsage() {
		return appCpuUsage;
	}

	public void setAppCpuUsage(float appCpuUsage) {
		this.appCpuUsage = appCpuUsage;
	}

	public float getFgct() {
		return fgct;
	}

	public void setFgct(float fgct) {
		this.fgct = fgct;
	}

	public float getGct() {
		return gct;
	}

	public void setGct(float gct) {
		this.gct = gct;
	}

	public float getPu() {
		return pu;
	}

	public void setPu(float pu) {
		this.pu = pu;
	}

	public float getNgcmn() {
		return ngcmn;
	}

	public void setNgcmn(float ngcmn) {
		this.ngcmn = ngcmn;
	}

	public float getNgcmx() {
		return ngcmx;
	}

	public void setNgcmx(float ngcmx) {
		this.ngcmx = ngcmx;
	}

	public float getNgc() {
		return ngc;
	}

	public void setNgc(float ngc) {
		this.ngc = ngc;
	}

	public float getS0c() {
		return s0c;
	}

	public void setS0c(float s0c) {
		this.s0c = s0c;
	}

	public float getS0u() {
		return s0u;
	}

	public void setS0u(float s0u) {
		this.s0u = s0u;
	}

	public float getS1c() {
		return s1c;
	}

	public void setS1c(float s1c) {
		this.s1c = s1c;
	}

	public float getS1u() {
		return s1u;
	}

	public void setS1u(float s1u) {
		this.s1u = s1u;
	}

	public float getEc() {
		return ec;
	}

	public void setEc(float ec) {
		this.ec = ec;
	}

	public float getEu() {
		return eu;
	}

	public void setEu(float eu) {
		this.eu = eu;
	}

	public float getOgcmn() {
		return ogcmn;
	}

	public void setOgcmn(float ogcmn) {
		this.ogcmn = ogcmn;
	}

	public float getOgcmx() {
		return ogcmx;
	}

	public void setOgcmx(float ogcmx) {
		this.ogcmx = ogcmx;
	}

	public float getOgc() {
		return ogc;
	}

	public void setOgc(float ogc) {
		this.ogc = ogc;
	}

	public float getOc() {
		return oc;
	}

	public void setOc(float oc) {
		this.oc = oc;
	}

	public float getOu() {
		return ou;
	}

	public void setOu(float ou) {
		this.ou = ou;
	}

	public float getMcmn() {
		return mcmn;
	}

	public void setMcmn(float mcmn) {
		this.mcmn = mcmn;
	}

	public float getMcmx() {
		return mcmx;
	}

	public void setMcmx(float mcmx) {
		this.mcmx = mcmx;
	}

	public float getMc() {
		return mc;
	}

	public void setMc(float mc) {
		this.mc = mc;
	}

	public float getCcsmn() {
		return ccsmn;
	}

	public void setCcsmn(float ccsmn) {
		this.ccsmn = ccsmn;
	}

	public float getCcsmx() {
		return ccsmx;
	}

	public void setCcsmx(float ccsmx) {
		this.ccsmx = ccsmx;
	}

	public float getCcsc() {
		return ccsc;
	}

	public void setCcsc(float ccsc) {
		this.ccsc = ccsc;
	}

	public float getYgc() {
		return ygc;
	}

	public void setYgc(float ygc) {
		this.ygc = ygc;
	}

	public float getFgc() {
		return fgc;
	}

	public void setFgc(float fgc) {
		this.fgc = fgc;
	}

	public float getMu() {
		return mu;
	}

	public void setMu(float mu) {
		this.mu = mu;
	}

	public float getPgcmn() {
		return pgcmn;
	}

	public void setPgcmn(float pgcmn) {
		this.pgcmn = pgcmn;
	}

	public float getPgcmx() {
		return pgcmx;
	}

	public void setPgcmx(float pgcmx) {
		this.pgcmx = pgcmx;
	}

	public float getPgc() {
		return pgc;
	}

	public void setPgc(float pgc) {
		this.pgc = pgc;
	}

	public float getPc() {
		return pc;
	}

	public void setPc(float pc) {
		this.pc = pc;
	}

}
