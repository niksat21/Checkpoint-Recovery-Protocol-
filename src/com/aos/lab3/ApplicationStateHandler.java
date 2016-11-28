package com.aos.lab3;

import java.util.LinkedList;
import java.util.List;

public class ApplicationStateHandler implements IApplicationStateHandler {

	private List<Integer> checkpointedValues = new LinkedList<Integer>();
	private List<Integer[]> FLS = new LinkedList<Integer[]>();
	private List<Integer[]> LLR = new LinkedList<Integer[]>();
	private List<Integer[]> LLS = new LinkedList<Integer[]>();

	@Override
	public List<Integer[]> getFLS() {
		return FLS;
	}

	@Override
	public List<Integer[]> getLLR() {
		return LLR;
	}

	@Override
	public List<Integer[]> getLLS() {
		return LLS;
	}

	@Override
	public List<Integer> getAppValues() {
		return checkpointedValues;
	}

	@Override
	public void storeFLS(Integer[] FLS) {
		this.FLS.add(FLS);
	}

	@Override
	public void storeLLR(Integer[] LLR) {
		this.LLR.add(LLR);
	}

	@Override
	public void storeLLS(Integer[] LLS) {
		this.LLS.add(LLS);
	}

	@Override
	public void storeAppValue(Integer value) {
		this.checkpointedValues.add(value);
	}

}
