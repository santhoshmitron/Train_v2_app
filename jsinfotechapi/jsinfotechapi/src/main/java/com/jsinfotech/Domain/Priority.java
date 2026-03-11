package com.jsinfotech.Domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Priority implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4927286812316099621L;

	Map<String,List<Long>> volumnes;
	public Map<String, List<Long>> getFivemvolumnes() {
		return fivemvolumnes;
	}
	public void setFivemvolumnes(Map<String, List<Long>> fivemvolumnes) {
		this.fivemvolumnes = fivemvolumnes;
	}
	Map<String,List<Long>> fivemvolumnes;

	public Map<String, List<Long>> getVolumnes() {
		return volumnes;
	}
	public void setVolumnes(Map<String, List<Long>> volumnes) {
		this.volumnes = volumnes;
	}
	public Map<String, List<Double>> getLtps() {
		return ltps;
	}
	public void setLtps(Map<String, List<Double>> ltps) {
		this.ltps = ltps;
	}
	public Map<String, Double> getVolumneAvg() {
		return volumneAvg;
	}
	public void setVolumneAvg(Map<String, Double> volumneAvg) {
		this.volumneAvg = volumneAvg;
	}
	public Map<String, Double> getLpsAvg() {
		return lpsAvg;
	}
	public void setLpsAvg(Map<String, Double> lpsAvg) {
		this.lpsAvg = lpsAvg;
	}
	Map<String,List<Double>> ltps;
	Map<String,Double> volumneAvg;
	Map<String,Double> lpsAvg;
	
	@Override
	public String toString() {
		return "Priority [volumnes=" + volumnes + ", ltps=" + ltps + ", volumneAvg=" + volumneAvg + ", lpsAvg=" + lpsAvg
				+ "]";
	}
	
	
}
