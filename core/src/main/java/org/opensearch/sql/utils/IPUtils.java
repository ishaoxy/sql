/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.utils;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IPAddressStringParameters;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;
import lombok.experimental.UtilityClass;
import org.opensearch.sql.exception.SemanticCheckException;

import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class IPUtils {

  // Parameters for IP address strings.
  private static final IPAddressStringParameters.Builder commonValidationOptions =
      new IPAddressStringParameters.Builder()
          .allowEmpty(false)
          .allowMask(false)
          .setEmptyAsLoopback(false)
          .allowPrefixOnly(false)
          .allow_inet_aton(false)
          .allowSingleSegment(false);

  private static final IPAddressStringParameters ipAddressStringParameters =
      commonValidationOptions.allowPrefix(false).toParams();
  private static final IPAddressStringParameters ipAddressRangeStringParameters =
      commonValidationOptions.allowPrefix(true).toParams();

  /**
   * Builds and returns the {@link IPAddress} represented by the given IP address range string in
   * CIDR (classless inter-domain routing) notation. Throws {@link SemanticCheckException} if it
   * does not represent a valid IP address range. Supports both IPv4 and IPv6 address ranges.
   */
  public static IPAddress toRange(String s) throws SemanticCheckException {
    try {
      IPAddress range = new IPAddressString(s, ipAddressRangeStringParameters).toAddress();

      // Convert IPv6 mapped address range to IPv4.
      if (range.isIPv4Convertible()) {
        final int prefixLength = range.getPrefixLength();
        range = range.toIPv4().setPrefixLength(prefixLength, false);
      }

      return range;

    } catch (AddressStringException e) {
      final String errorFormat = "IP address range string '%s' is not valid. Error details: %s";
      throw new SemanticCheckException(String.format(errorFormat, s, e.getMessage()), e);
    }
  }

  /**
   * Builds and returns the {@link IPAddress} represented to the given IP address string. Throws
   * {@link SemanticCheckException} if it does not represent a valid IP address. Supports both IPv4
   * and IPv6 addresses.
   */
  public static IPAddress toAddress(String s) throws SemanticCheckException {
    try {
      IPAddress address = new IPAddressString(s, ipAddressStringParameters).toAddress();

      // Convert IPv6 mapped address to IPv4.
      if (address.isIPv4Convertible()) {
        address = address.toIPv4();
      }

      return address;
    } catch (AddressStringException e) {
      final String errorFormat = "IP address string '%s' is not valid. Error details: %s";
      throw new SemanticCheckException(String.format(errorFormat, s, e.getMessage()), e);
    }
  }

  /**
   * Compares the given {@link IPAddress} objects for order. Returns a negative integer, zero, or a
   * positive integer if the first {@link IPAddress} object is less than, equal to, or greater than
   * the second one. IPv4 addresses are mapped to IPv6 for comparison.
   */
  public static int compare(IPAddress a, IPAddress b) {
    final IPv6Address ipv6A = toIPv6Address(a);
    final IPv6Address ipv6B = toIPv6Address(b);

    return ipv6A.compareTo(ipv6B);
  }

  /** Returns the {@link IPv6Address} corresponding to the given {@link IPAddress}. */
  private static IPv6Address toIPv6Address(IPAddress ipAddress) {
    return ipAddress instanceof IPv4Address iPv4Address
        ? iPv4Address.toIPv6()
        : (IPv6Address) ipAddress;
  }

  /**
   * Formats the IP address into a standard expanded form:
   * - IPv4: pads each octet to 3 digits, e.g., "1.2.3.4" -> "001.002.003.004"
   * - IPv6: expands to full form with 4-digit groups, e.g., "2001:db8::1" -> "2001:0db8:0000:0000:0000:0000:0000:0001"
   */
  public static String formatIPAddress(String s) throws SemanticCheckException {
    try {
      IPAddress address = new IPAddressString(s, ipAddressStringParameters).toAddress();

      if (address.isIPv4()) {
        // Format as 001.002.003.004
        IPv4Address ipv4 = address.toIPv4();
        return Arrays.stream(ipv4.getSegments())
            .map(seg -> String.format("%03d", seg.getSegmentValue()))
            .collect(Collectors.joining("."));
      } else if (address.isIPv6()) {
        // Format as full 4-digit groups
        IPv6Address ipv6 = address.toIPv6();
        return Arrays.stream(ipv6.getSegments())
            .map(seg -> String.format("%04x", seg.getSegmentValue()))
            .collect(Collectors.joining(":"));
      } else {
        return s;  // fallback
      }
    } catch (AddressStringException e) {
      throw new SemanticCheckException(String.format("IP address '%s' is invalid: %s", s, e.getMessage()), e);
    }
  }

}
