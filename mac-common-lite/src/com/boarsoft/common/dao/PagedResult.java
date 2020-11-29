package com.boarsoft.common.dao;

import java.io.Serializable;
import java.util.List;

public class PagedResult<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<T> list;
	private long total = 0;
	// 与原有的MAC框架兼容
	private int pageNo = 0;
	private int pageSize = 50;
	private long pageCount = 0;

	/**
	 * ExtJs 需要的
	 * 
	 * @param total
	 * @param list
	 */
	public PagedResult(long total, List<T> list) {
		this.setTotal(total);
		this.setList(list);
	}

	public PagedResult(long total, List<T> list, int pageNo, int pageSize) {
		this.total = total;
		this.list = list;
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.pageCount = (total + pageSize - 1) / pageSize;
	}

	public List<T> getList() {
		return list;
	}

	public long getPageCount() {
		return pageCount;
	}

	public void setPageCount(long pageCount) {
		this.pageCount = pageCount;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}
}