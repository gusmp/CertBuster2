package org.gusmp.CertBuster2.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.stereotype.Component;

@Component
public class IpUtils {

	public String getIPFromLong(final long ipAslong) {
		return String.format("%d.%d.%d.%d", (ipAslong >>> 24) & 0xff, (ipAslong >>> 16) & 0xff, (ipAslong >>> 8) & 0xff, (ipAslong) & 0xff);
	}

	public Long getLongFromIP(final String ipAsString) {

		String components[] = ipAsString.split("\\.");
		// a*256^3 + b*256^2 + c*256^1 + d
		return Integer.parseInt(components[0]) * 16777216L + Integer.parseInt(components[1]) * 65536 + Integer.parseInt(components[2]) * 256
				+ Integer.parseInt(components[3]);
	}

	public int getIntFromIp(final String ipAsString) throws UnknownHostException {

		Inet4Address a = (Inet4Address) InetAddress.getByName(ipAsString);
		byte[] b = a.getAddress();
		int i = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | ((b[3] & 0xFF) << 0);

		return i;
	}

	public long numerOfHosts(String subnet) {

		String[] components = subnet.split("/");

		int t = 32 - Integer.parseInt(components[1]);
		long numberOfHosts = (long) Math.pow(2, t) - 2;
		if (numberOfHosts < 0)
			numberOfHosts = 0;

		return numberOfHosts;

	}

}
