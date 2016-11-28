package com.aos.lab3;

import java.util.List;

public interface IApplicationStateHandler {

	public List<Integer[]> getFLS();

	public List<Integer[]> getLLR();

	public List<Integer[]> getLLS();

	public List<Integer> getAppValues();

	public void storeFLS(Integer[] FLS);

	public void storeLLR(Integer[] LLR);

	public void storeLLS(Integer[] LLS);

	public void storeAppValue(Integer value);

}
