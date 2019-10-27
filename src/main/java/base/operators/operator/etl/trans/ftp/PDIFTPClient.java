package base.operators.operator.etl.trans.ftp;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;

import java.io.IOException;
import java.text.ParseException;

public class PDIFTPClient extends FTPClient {

  /**
   * MDTM supported flag
   */
  private boolean mdtmSupported = true;

  /**
   * SIZE supported flag
   */
  private boolean sizeSupported = true;

  private static Class<?> PKG = PDIFTPClient.class; // for i18n purposes, needed by Translator2!!

  public PDIFTPClient( ) {
    super();
  }

  /*
   * (non-Javadoc)
   *
   * @see com.enterprisedt.net.ftp.FTPClientInterface#exists(java.lang.String)
   */
  public boolean exists( String remoteFile ) throws IOException, FTPException {
    checkConnection( true );

    // first try the SIZE command
    if ( sizeSupported ) {
      lastReply = control.sendCommand( "SIZE " + remoteFile );
      char ch = lastReply.getReplyCode().charAt( 0 );
      if ( ch == '2' ) {
        return true;
      }
      if ( ch == '5' && fileNotFoundStrings.matches( lastReply.getReplyText() ) ) {
        return false;
      }

      sizeSupported = false;
    }

    // then try the MDTM command
    if ( mdtmSupported ) {
      lastReply = control.sendCommand( "MDTM " + remoteFile );
      char ch = lastReply.getReplyCode().charAt( 0 );
      if ( ch == '2' ) {
        return true;
      }
      if ( ch == '5' && fileNotFoundStrings.matches( lastReply.getReplyText() ) ) {
        return false;
      }

      mdtmSupported = false;
    }

    try {
      FTPFile[] files = dirDetails( null ); // My fix - replace "." with null in this call for MVS support
      for ( int i = 0; i < files.length; i++ ) {
        if ( files[i].getName().equals( remoteFile ) ) {
          return files[i].isFile();
        }
      }
      return false;
    } catch ( ParseException ex ) {
      ex.printStackTrace();
      return false;
    }
  }

}
