package com.ics.cerebrum.nodes.xml;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 15-Feb-2022
 */
@XmlRootElement(name = "subscriptions")
public class Subscriptions 
{
	private List<Subscription> subscriptionList;

	@XmlElement(name = "subscription")
	public List<Subscription> getSubscriptionList() {
		return subscriptionList;
	}

	public void setSubscriptionList(List<Subscription> subscriptionList) {
		this.subscriptionList = subscriptionList;
	}
}
