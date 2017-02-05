package org.gusmp.CertBuster2.beans;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.net.util.SubnetUtils;
import org.gusmp.CertBuster2.service.WrongScanFileEntryException;

import lombok.Cleanup;
import lombok.Getter;

public class NetworkScanFileEntry implements IScanFileEntry {

	@Getter
	public class PortRange {

		private int start;
		private int end;

		public PortRange(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	private String networkEntry;

	@Getter
	private List<PortRange> portRangeList = new ArrayList<PortRange>();

	@Getter
	private SubnetUtils hostRange;

	public NetworkScanFileEntry(String networkEntry) throws WrongScanFileEntryException {
		this.networkEntry = networkEntry;

		String components[] = networkEntry.split(":");
		if (components.length > 2) {
			throw new WrongScanFileEntryException("Network entry " + networkEntry + " has too many ':'");
		}

		validateHostSubnet(components[0]);
		if (components.length == 1) {
			// add default https port
			portRangeList.add(new PortRange(443, 443));
		} else {
			validatePortList(components[1]);
		}
	}

	private void validatePortList(String portList) throws WrongScanFileEntryException {

		@Cleanup
		Scanner scanner = new Scanner(portList);
		scanner.useDelimiter(",");

		while (scanner.hasNext()) {

			try {
				int port = scanner.nextInt();
				validatePort(port);
				portRangeList.add(new PortRange(port, port));
				continue;
			} catch (Exception exc) {
			}

			try {
				String group = scanner.next("\\d+-\\d+");
				int start = Integer.parseInt(group.split("-")[0]);
				int end = Integer.parseInt(group.split("-")[1]);
				if (start >= end)
					throw new Exception();
				validatePort(start);
				validatePort(end);
				portRangeList.add(new PortRange(start, end));
				continue;
			} catch (Exception exc) {
			}

			scanner.close();
			throw new WrongScanFileEntryException("Port list " + portList
					+ " is invalid. Or it is not and integer, or falls into 1-65535 or is not a valid port range (port1-port2 where port1 < port2)");

		}
	}

	private void validatePort(int port) throws WrongScanFileEntryException {

		if ((port < 1) || (port > 65535)) {
			throw new WrongScanFileEntryException("Port " + port + " is out of bounds");
		}
	}

	private void validateHostSubnet(String hostOrSubnet) throws WrongScanFileEntryException {

		if (hostOrSubnet.indexOf("/") > 0) {
			validateSubnet(hostOrSubnet);
		} else {
			try {
				validateSubnet(InetAddress.getByName(hostOrSubnet).getHostAddress() + "/32");
			} catch (UnknownHostException exc) {
				throw new WrongScanFileEntryException(exc.toString());
			}
		}
	}

	private void validateSubnet(String cidrNotation) throws WrongScanFileEntryException {

		try {
			hostRange = new SubnetUtils(cidrNotation);
		} catch (Exception exc) {
			throw new WrongScanFileEntryException(cidrNotation + " is not a valid subnet");
		}
	}

	/*
	 * private void validateHost(String host) throws WrongScanFileEntryException
	 * {
	 * 
	 * try { InetAddress.getByName(host); } catch(Exception exc) { throw new
	 * WrongScanFileEntryException(host + " is not a valid host name"); } }
	 */

	@Override
	public ENTRY_TYPE getEntryType() {
		return ENTRY_TYPE.NETWORK;
	}

	@Override
	public String getEntry() {
		return networkEntry;
	}

}
