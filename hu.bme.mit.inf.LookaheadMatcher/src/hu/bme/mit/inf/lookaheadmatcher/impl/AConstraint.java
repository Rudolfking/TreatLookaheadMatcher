package hu.bme.mit.inf.lookaheadmatcher.impl;

import hu.bme.mit.inf.lookaheadmatcher.IDelta;

import java.util.ArrayList;
import java.util.List;

public class AConstraint implements IConstraint {

	protected List<IDelta> mailBox = null;
	
	public void putToMailbox(IDelta delta)
	{
		if (mailBox == null)
			mailBox = new ArrayList<IDelta>();
		mailBox.add(delta);
	}
	
	public boolean removeFromMailbox(IDelta delta)
	{
		if (mailBox == null)
			return false;
		return mailBox.remove(delta);
	}
	
	public List<IDelta> getMailboxContent()
	{
		return mailBox;
	}
	
	public boolean hasMailboxContent()
	{
		return mailBox != null && mailBox.size() > 0;
	}
}
