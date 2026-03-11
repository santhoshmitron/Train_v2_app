package com.jsinfotech.Domain;

public class Gate2 {

	public Gate2(String gateNum, String boom1_id, String g_value,String l_value) {
        this.gateNum = gateNum;
        this.boom1Id = boom1_id;
        this.g_value = g_value;
        this.l_value = l_value;
    }
	public String getMvbuy() {
		return mvbuy;
	}

	public void setMvbuy(String mvbuy) {
		this.mvbuy = mvbuy;
	}

	public String getTechin() {
		return techin;
	}

	public void setTechin(String techin) {
		this.techin = techin;
	}

	public String getFivem() {
		return fivem;
	}

	public void setFivem(String fivem) {
		this.fivem = fivem;
	}

	public String getOnemin() {
		return onemin;
	}

	public void setOnemin(String onemin) {
		this.onemin = onemin;
	}

	public String getBuy() {
		return buy;
	}

	public void setBuy(String buy) {
		this.buy = buy;
	}

	public String getSell() {
		return sell;
	}

	public void setSell(String sell) {
		this.sell = sell;
	}

	public String getPercentage() {
		return percentage;
	}

	public void setPercentage(String percentage) {
		this.percentage = percentage;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getSubcategory() {
		return subcategory;
	}

	public void setSubcategory(String subcategory) {
		this.subcategory = subcategory;
	}
	public String mvbuy;
    public String techin;
    public String fivem;
    public String onemin;
    public String buy;
    public String sell;
    public String percentage;
    public String category;
    public String subcategory;
    public String getVolumnechange() {
		return volumnechange;
	}
	public void setVolumnechange(String volumnechange) {
		this.volumnechange = volumnechange;
	}
	public String volumnechange;
    public String getFilter() {
		return filter;
	}
	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String filter;
    
    public String getVolumne() {
		return volumne;
	}
	public void setVolumne(String volumne) {
		this.volumne = volumne;
	}
	public String getOnechgvolumne() {
		return onechgvolumne;
	}
	public void setOnechgvolumne(String onechgvolumne) {
		this.onechgvolumne = onechgvolumne;
	}
	public String volumne;
    public String onechgvolumne;
    
    public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	private String timeStamp;
    public Priority getPriority() {
		return priority;
	}
	public void setPriority(Priority priority) {
		this.priority = priority;
	}
	private Priority priority;
    public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	private String text;
	public String getS1() {
		return s1;
	}
	public void setS1(String s1) {
		this.s1 = s1;
	}
	private String s1;

    private String gateNum;
    private String handle;
    private String lever;
    public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getBatch() {
		return batch;
	}

	public void setBatch(String batch) {
		this.batch = batch;
	}

	private String date;
    private String batch;

    public String getL_value() {
        return l_value;
    }

    public void setL_value(String l_value) {
        this.l_value = l_value;
    }

    public String getG_value() {
        return g_value;
    }

    public void setG_value(String g_value) {
        this.g_value = g_value;
    }

    private String l_value;
    private String g_value;


    public String getBoom1Id() {
        return boom1Id;
    }

    public void setBoom1Id(String boom1Id) {
        this.boom1Id = boom1Id;
    }

    private String boom1Id;

    public String getGateNum() {
        return gateNum;
    }

    public void setGateNum(String gateNum) {
        this.gateNum = gateNum;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getLever() {
        return lever;
    }

    public void setLever(String lever) {
        this.lever = lever;
    }
	@Override
	public String toString() {
		return "Gate [mvbuy=" + mvbuy + ", techin=" + techin + ", fivem=" + fivem + ", onemin=" + onemin + ", buy="
				+ buy + ", sell=" + sell + ", percentage=" + percentage + ", category=" + category + ", subcategory="
				+ subcategory + ", volumnechange=" + volumnechange + ", filter=" + filter + ", volumne=" + volumne
				+ ", onechgvolumne=" + onechgvolumne + ", timeStamp=" + timeStamp + ", priority=" + priority + ", text="
				+ text + ", s1=" + s1 + ", gateNum=" + gateNum + ", handle=" + handle + ", lever=" + lever + ", date="
				+ date + ", batch=" + batch + ", l_value=" + l_value + ", g_value=" + g_value + ", boom1Id=" + boom1Id
				+ "]";
	}


}
