/*
 * Copyright 2007 Thomas Stock.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors:
 * 
 */
package net.sourceforge.jwbf.contentRep.mw;

public class CategoryItem {
	
	private String title = "";
	private int namespace;
	private int pageid;
	
	
	public String toString() {
		return title;
	}


	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}


	public int getNamespace() {
		return namespace;
	}


	public void setNamespace(int namespace) {
		this.namespace = namespace;
	}


	public int getPageid() {
		return pageid;
	}


	public void setPageid(int pageid) {
		this.pageid = pageid;
	}

}
