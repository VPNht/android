package ht.vpn.android.content;

import android.content.SharedPreferences;

import java.util.ArrayList;

import ht.vpn.android.Preferences;
import ht.vpn.android.network.responses.Server;

public class VPNHTConfig {

    public static String generate(SharedPreferences preferences, Server server, Boolean firewall) {
        ArrayList<String> stringList = new ArrayList<>();

        stringList.add("client");
        stringList.add("dev tun");

        if(firewall) {
            stringList.add("proto tcp");
        } else {
            stringList.add("proto udp");
        }

        stringList.add("resolv-retry infinite");
        stringList.add("nobind");
        stringList.add("key-direction 1");
        stringList.add("reneg-sec 0");
        stringList.add("tun-mtu 1500");
        stringList.add("tun-mtu-extra 32");
        stringList.add("mssfix 1450");
        stringList.add("persist-key");

        if(preferences.getBoolean(Preferences.KILLSWITCH, true)) {
            stringList.add("persist-tun");
        }

        stringList.add("ping 15");
        stringList.add("ping-restart 45");
        stringList.add("ping-timer-rem");
        stringList.add("ns-cert-type server");
        stringList.add("mute 10");
        stringList.add("comp-lzo");
        stringList.add("verb 3");
        stringList.add("pull");
        stringList.add("fast-io");
        stringList.add("auth-nocache");

        if(!preferences.getBoolean(Preferences.SMARTDNS, true)) {
            stringList.add("route-nopull");
            stringList.add("redirect-gateway def1");
            stringList.add("dhcp-option DNS 10.11.0.1");
        }

        if(firewall) {
            stringList.add(String.format("cipher %s", "AES-128-CBC"));
        } else {
            stringList.add(String.format("cipher %s", preferences.getString(Preferences.ENC_TYPE, "AES-128-CBC")));
            stringList.add("explicit-exit-notify");
        }

        stringList.add("remote-random");

        stringList.add("<auth-user-pass>");
        stringList.add(preferences.getString(Preferences.USERNAME, ""));
        stringList.add(preferences.getString(Preferences.PASSWORD, ""));
        stringList.add("</auth-user-pass>");


        stringList.add("<ca>");
        stringList.add("-----BEGIN CERTIFICATE-----");
        stringList.add("MIIEmzCCA4OgAwIBAgIJAIsPF0BTVr9FMA0GCSqGSIb3DQEBCwUAMIGPMQswCQYD");
        stringList.add("VQQGEwJVUzELMAkGA1UECBMCREUxEzARBgNVBAcTCldpbG1pbmd0b24xDjAMBgNV");
        stringList.add("BAoTBVZwbkhUMQ4wDAYDVQQLEwVWUE5IVDEPMA0GA1UEAxMGdnBuLmh0MQ4wDAYD");
        stringList.add("VQQpEwVWUE5IVDEdMBsGCSqGSIb3DQEJARYOc3VwcG9ydEB2cG4uaHQwHhcNMTQx");
        stringList.add("MTI4MTM1NDE5WhcNMjQxMTI1MTM1NDE5WjCBjzELMAkGA1UEBhMCVVMxCzAJBgNV");
        stringList.add("BAgTAkRFMRMwEQYDVQQHEwpXaWxtaW5ndG9uMQ4wDAYDVQQKEwVWcG5IVDEOMAwG");
        stringList.add("A1UECxMFVlBOSFQxDzANBgNVBAMTBnZwbi5odDEOMAwGA1UEKRMFVlBOSFQxHTAb");
        stringList.add("BgkqhkiG9w0BCQEWDnN1cHBvcnRAdnBuLmh0MIIBIjANBgkqhkiG9w0BAQEFAAOC");
        stringList.add("AQ8AMIIBCgKCAQEA3Vz35G5+cChwgyy2L96U6hVCkT2TVfXE4EA+UoVzSV2DQIYH");
        stringList.add("4cdz+t+jmvyfPZCLdqRIZ3QgV+ftPFuCPlrESdccIOhe1KM5GDDv4LhsQC6jbAsH");
        stringList.add("pmRrilIxLyZBVTfe2opAJ1A1e03CYjORgLBz7vx+krQ+cG6p7mR/aoKSJulgOhkR");
        stringList.add("PffXKhnq7dGQkFR5tSG5xESsQVbRqP82xyR9eCOC8GyN6yKt85vCmA/e+6f3fGrJ");
        stringList.add("uyXnmexxrV8GvRNbdScY/TcjaPsMwePOaOGfa97Svt/M7loTKgUI544p+nEH3QeK");
        stringList.add("BxwiryBUOHmFetOBh+2nabn5982t4k+MVdy6kQIDAQABo4H3MIH0MB0GA1UdDgQW");
        stringList.add("BBRvE5Y9ivf8/XYJosCQJeaOIhnYBTCBxAYDVR0jBIG8MIG5gBRvE5Y9ivf8/XYJ");
        stringList.add("osCQJeaOIhnYBaGBlaSBkjCBjzELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAkRFMRMw");
        stringList.add("EQYDVQQHEwpXaWxtaW5ndG9uMQ4wDAYDVQQKEwVWcG5IVDEOMAwGA1UECxMFVlBO");
        stringList.add("SFQxDzANBgNVBAMTBnZwbi5odDEOMAwGA1UEKRMFVlBOSFQxHTAbBgkqhkiG9w0B");
        stringList.add("CQEWDnN1cHBvcnRAdnBuLmh0ggkAiw8XQFNWv0UwDAYDVR0TBAUwAwEB/zANBgkq");
        stringList.add("hkiG9w0BAQsFAAOCAQEAOyV3OXQOyJk4U4kkLtvy/Kw0p2V3kaAwRZ9t8sQU1vm4");
        stringList.add("g/5DIE3lbfCKT4vyb1ckzoV6bP6lG/9NhePJyGR6kub1M9KmwbdR68uTXH69S8/N");
        stringList.add("ENdjI66gcPLmZGB7FrlMV7wQUy7X5g3cbLJ6spVKqM7lnYmxSqfwTG8qq546gdgk");
        stringList.add("0OcROxPVtRDyKr+xQRg+WJSFa1ugcVz/x2FiYyTXFwgTS9RAXymTOiDIZcTlrmik");
        stringList.add("32XQSJBk1cbUDCFsZo9LbuUB3Oe6Kv36wUJAXlsxgEtdgEcsr7BezqLcSPp6PyqC");
        stringList.add("5GZ97ULagirc82d4BfDVp1GtUJlJMLJVMAmaoNn3Sw==");
        stringList.add("-----END CERTIFICATE-----");
        stringList.add("</ca>");

        stringList.add("<tls-auth>");
        stringList.add("-----BEGIN OpenVPN Static key V1-----");
        stringList.add("74fa428696037279b617bb92efc1d2df");
        stringList.add("edf3e030b0e24b848e1389490411e2b6");
        stringList.add("ebbc521669285d17b9aeea190066502a");
        stringList.add("c3ad09b0b272a81ed737760451fe6071");
        stringList.add("a2003356a5f8e0f8f4555290f539bcfb");
        stringList.add("371282cec7f6de53ffce1665f304f774");
        stringList.add("6d4aaad012afa02a4faa9d4db325e104");
        stringList.add("e1c957b056e1d6130daf4210531488e0");
        stringList.add("978ba4ddaac3986e31c23f6589d21f62");
        stringList.add("e36354931f0723771376c117b6ef3a17");
        stringList.add("260e1f582475b8e1438147a82d716b37");
        stringList.add("f8d451f0191586040950721bc5657657");
        stringList.add("ecd7574731c06d390af2977c2eb15176");
        stringList.add("b604121698394edf94e1ea091f008b83");
        stringList.add("ad7921e7beba7b175956b9261d0cd686");
        stringList.add("692b07de56806b72e46e5a7a69f9bb9a");
        stringList.add("-----END OpenVPN Static key V1-----");
        stringList.add("</tls-auth>");

        if(firewall) {
            stringList.add(String.format("remote %s %s", server.hostname, "443"));
        } else {
            for (String port : getPortsByProtocol(preferences.getString(Preferences.ENC_TYPE, "AES-128-CBC"))) {
                stringList.add(String.format("remote %s %s", server.hostname, port));
            }
        }
        // Test server:
        // stringList.add(String.format("remote %s %s", "188.226.230.75", "1194"));

        StringBuilder builder = new StringBuilder();
        for(String s : stringList) {
            builder.append(s);
            builder.append('\n');
        }
        return builder.toString();
    }

    private static String[] getPortsByProtocol(String protocol) {
        ArrayList<String> stringList = new ArrayList<>();
        switch(protocol) {
            case "AES-256-CBC":
                for(int i = 1300; i < 1305; i++) {
                    stringList.add(Integer.toString(i));
                }
                break;
            case "AES-128-CBC":
                for(int i = 1194; i < 1201; i++) {
                    stringList.add(Integer.toString(i));
                }
                break;
            case "BF-CBC":
                for(int i = 1202; i < 1206; i++) {
                    stringList.add(Integer.toString(i));
                }
                break;
        }
        return stringList.toArray(new String[stringList.size()]);
    }

}
