/***
 * * REDS - REconfigurable Dispatching System
 * * Copyright (C) 2003 Politecnico di Milano
 * * <mailto: cugola@elet.polimi.it> <mailto: picco@elet.polimi.it>
 * *
 * * This library is free software; you can redistribute it and/or modify it
 * * under the terms of the GNU Lesser General Public License as published by
 * * the Free Software Foundation; either version 2.1 of the License, or (at
 * * your option) any later version.
 * *
 * * This library is distributed in the hope that it will be useful, but
 * * WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * * General Public License for more details.
 * *
 * * You should have received a copy of the GNU Lesser General Public License
 * * along with this library; if not, write to the Free Software Foundation,
 * * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 ***/

package polimi.reds.broker.overlay;

import java.io.Serializable;

/**
 * The envelope for data sent and received by REDS <code>Transport</code>s. Each envelope
 * includes a subject and a payload (any <code>Serializable</code> object)
 * 
 * @see polimi.reds.broker.overlay.Transport
 * @see polimi.reds.broker.overlay.Link
 * @see polimi.reds.broker.overlay.DataListener
 */
class Envelope implements Serializable {
  private static final long serialVersionUID = -6474079527819041575L;
  /**
   * The default subject.
   */
  public static final String DEFAULT = "DEFAULT_SBJ";
  private String subject;
  private Serializable payload;

  /**
   * Builds a new, empty envelope with the default subject.
   */
  public Envelope() {
    this(DEFAULT, null);
  }

  /**
   * Builds a new envelope with the given subject and payload.
   * 
   * @param subject The subject of the new envelope.
   * @param payload The payload of the new envelope.
   */
  public Envelope(String subject, Serializable payload) {
    this.subject = subject;
    this.payload = payload;
  }

  /**
   * @return Returns the payload.
   */
  public Serializable getPayload() {
    return payload;
  }

  /**
   * @param payload The payload to set.
   */
  public void setPayload(Serializable payload) {
    this.payload = payload;
  }

  /**
   * @return Returns the subject.
   */
  public String getSubject() {
    return subject;
  }

  /**
   * @param subject The subject to set.
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }
}
