package base.operators.operator.etl.trans.ftp;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import org.junit.Assert;

import java.io.IOException;
import java.net.InetAddress;

public class FTPUtils {

    public static FTPClient createFtpClient() {
        return new PDIFTPClient();
    }
    // package-local visibility for testing purposes
    public static FTPClient createAndSetUpFtpClient(String serverName, String serverPort, String userName, String password, String proxyHost, String proxyPort, String proxyUsername, String proxyPassword, boolean activeConnection, int timeout, String controlEncoding, String socksProxyHost, String socksProxyPort, String socksProxyUsername, String socksProxyPassword) throws IOException, FTPException {

        FTPClient ftpClient = createFtpClient();
        ftpClient.setRemoteAddr( InetAddress.getByName( serverName ) );
        ftpClient.setRemotePort( Integer.parseInt(serverPort) );

        if ( !(proxyHost==null) ) {
            ftpClient.setRemoteAddr( InetAddress.getByName( proxyHost ) );
            // FIXME: Proper default port for proxy
            int port = Integer.parseInt(proxyPort);
            if ( port != 0 ) {
                ftpClient.setRemotePort( port );
            }
        }

        // set activeConnection connectmode ...
        if ( activeConnection ) {
            ftpClient.setConnectMode( FTPConnectMode.ACTIVE );
        } else {
            ftpClient.setConnectMode( FTPConnectMode.PASV );
        }

        // Set the timeout
        if ( timeout > 0 ) {
            ftpClient.setTimeout( timeout );
        }

        ftpClient.setControlEncoding( controlEncoding );
        // If socks proxy server was provided
        if ( !( socksProxyHost==null ) ) {
            // if a port was provided
            if ( !( socksProxyPort==null ) ) {
                FTPClient.initSOCKS(socksProxyPort, socksProxyHost);
            }else {
                Assert.fail("Have a username without a password or vica versa.");
            }
            // now if we have authentication information
            if ( !(socksProxyUsername==null) && !( socksProxyPassword==null ) ) {
                FTPClient.initSOCKSAuthentication(socksProxyUsername,socksProxyPassword);
            } else if ( !(socksProxyUsername==null)
                    && ( socksProxyPassword==null ) || (socksProxyUsername==null)
                    && !( socksProxyPassword==null )) {
                // we have a username without a password or vica versa
                Assert.fail("FTP socks proxy is incomplete credentials.");
            }
        }

        // login to ftp host ...
        ftpClient.connect();

        String realUsername = userName+(!(proxyHost==null) ? "@" + serverName : "" )+(!(proxyUsername==null)? " "+proxyUsername:"" );

        String realPassword = password+(!( proxyPassword == null) ? " "+proxyPassword : "" );

        ftpClient.login( realUsername, realPassword );

        return ftpClient;
    }


}
