package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response;

/**
 * This class represents a plugin response provided by VideoServer
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
 */
public interface VideoServerPluginResponse {

  /**
   * Gets info about the status of the response
   *
   * @return true if the response is successful, false otherwise
   */
  boolean statusOK();

  /**
   * Gets the room identifier from the response if it's successful
   *
   * @return room id
   */
  String getRoom();
}
