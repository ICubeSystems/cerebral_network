package com.ics.cerebrum.nodes.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 14-Feb-2022
 */
@XmlRootElement(name = "synapticNodes")
public class SynapticNodes 
{
	private List<SynapticNode> nodes;

	@XmlElement(name = "synapticNode")
	public List<SynapticNode> getNodes() {
		return nodes;
	}
	
	public void setNodes(List<SynapticNode> nodes) {
		this.nodes = nodes;
	}
}
