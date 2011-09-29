package com.shtao.ext.ibatis.common.util;

import java.util.List;

public interface PhysicalPaginatedList<E> {

	/**
	 * 得到当前列表
	 * @return
	 */
	public List<E> getPageList();
	
	/**
	 * 当前页码
	 * 
	 * @return
	 */
	public int getPageIndex();

	/**
	 * 总行数
	 */
	public int getTotalCount();

	/**
	 * 总页数
	 * 
	 * @return
	 */
	public int getPageCount();

	/**
	 * 每页行数
	 * 
	 * @return
	 */
	public int getPageSize();


	/**
	 * 是否是第一页
	 * 
	 * @return
	 */
	public boolean isFirstPage();

	/**
	 * 是否是最后一页
	 * 
	 * @return
	 */
	public boolean isLastPage();

	/**
	 * 前一页数
	 * 
	 * @return
	 */
	public int getPreviousPageIndex();

	/**
	 * 后一页数
	 * 
	 * @return
	 */
	public int getNextPageIndex();
}
