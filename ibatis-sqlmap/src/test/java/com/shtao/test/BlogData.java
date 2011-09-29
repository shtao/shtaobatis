package com.shtao.test;

public class BlogData {
	protected String id = null;
	protected String name = null;
	protected int rating = 0;
	
	public BlogData() {
	}
	
	public BlogData(String id, String name, int rating) {
		this.id = id;
		this.name = name;
		this.rating = rating;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}
}
