package hu.bme.mit.inf.lookaheadmatcher.impl;

import hu.bme.mit.inf.lookaheadmatcher.IDelta;

import java.util.ArrayList;
import java.util.List;

public abstract class AxisConstraint implements IConstraint
{
	public boolean isAxis()
	{
		return true;
	}
	public boolean isCheckable()
	{
		return false;
	}
	
	protected List<IDelta> mailBox = new ArrayList<IDelta>();
	
	public void putToMailbox(IDelta delta)
	{
		mailBox.add(delta);
	}
	
	public List<IDelta> getMailboxContent()
	{
		return mailBox;
	}
	
	public boolean hasDeltas()
	{
		return mailBox.size() > 0;
	}
}
