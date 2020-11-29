package com.boarsoft.rpc.bean;

public class RpcPackage {
	/** 当前RPC请求或响应数据包的分包总数 */
	protected int totalPackage = 0;
	/** 已收到的分包数 */
	protected int packageCount = 0;
	/** 已收到数据（由各分包中的数据累积而来） */
	protected byte[] data = null;
	/** 数据写入索引，表示下一分包数据到来后，应从data数组的第几位开始写入 */
	protected int writeIndex = 0;
	/** （数据）协议，通常表示反序化的方式 */
	protected int protocol = 0;
	
	public void moveWriteIndex(int bodyLength) {
		writeIndex += bodyLength;
	}

	public void plusPackageCount() {
		packageCount++;
	}

	public boolean isCompleted() {
		return packageCount == totalPackage;// && writeIndex == data.length;
	}

	public RpcPackage() {
	}

	public int getTotalPackage() {
		return totalPackage;
	}

	public void setTotalPackage(int totalPackage) {
		this.totalPackage = totalPackage;
	}

	public int getPackageCount() {
		return packageCount;
	}

	public void setPackageCount(int packageCount) {
		this.packageCount = packageCount;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public int getWriteIndex() {
		return writeIndex;
	}

	public void setWriteIndex(int writeIndex) {
		this.writeIndex = writeIndex;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}
}
