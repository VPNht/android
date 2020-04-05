package ht.vpn.android.api;

/**
 * Example of a callback interface used by IRemoteService to send
 * synchronous notifications back to its clients.  Note that this is a
 * one-way interface so the server does not block waiting for the client.
 */
oneway interface IOpenVPNStatusCallback {
    /**
     * Called when the service has a new status for you.
     */
    void newStatus(String uuid, String state, String message, String level);
}
