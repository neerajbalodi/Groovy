import java.util.Collection
import java.util.ArrayList
import java.util.Collection
import java.util.Date
import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpATTRS
import com.jcraft.jsch.SftpException
import java.nio.charset.StandardCharsets

flowFile = session.get()
if (!flowFile) return
try{
	List<String> ls = new ArrayList<>();
	JSch jsch = new JSch()
	Session sessionId =null
	String url = flowFile.getAttribute('HOST_URL')
	String password = flowFile.getAttribute('PASSWORD')
	String useName = flowFile.getAttribute('USER_NAME')
	String path = flowFile.getAttribute('PATH')
	    sessionId = jsch.getSession(useName, url, 22)
            sessionId.setConfig("StrictHostKeyChecking", "no")
            sessionId.setPassword(password)
            sessionId.connect()
			Channel channel = sessionId.openChannel("sftp")
            		channel.connect()
			ChannelSftp sftpChannel = (ChannelSftp) channel
			SftpATTRS attr =  sftpChannel.stat(path);
			folderDelete(sftpChannel,path,ls)
			sftpChannel.exit()
            		sessionId.disconnect()
		flowFile = session.putAttribute(flowFile, "Total File", Integer.toString(ls.size()))
		StringBuilder sb = new StringBuilder();
for (String s : ls)
{
    sb.append(s);
    sb.append("\t");
}
// Cast a closure with an inputStream and outputStream parameter to StreamCallback
        
        flowFile = session.write(flowFile, {outputStream ->
  outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8))
} as OutputStreamCallback)
        
		session.transfer(flowFile, REL_SUCCESS)
}
		catch (Exception e) {
 		flowFile = session.putAttribute(flowFile, "Error", e.getMessage())
 		session.transfer(flowFile, REL_FAILURE)
		}
 void folderDelete(ChannelSftp sftpChannel, String path,List ls) throws SftpException {	
		Collection<ChannelSftp.LsEntry> fileAndFolderList = sftpChannel.ls(path)
		for (LsEntry item : fileAndFolderList) {
			if ((new Date().getTime() - (new Date(item.getAttrs().getMTime() * 1000L).getTime()))
					/ (60 * 60 * 24 * 1000) > 60L || (new Date().getTime()-item.getAttrs().getMTime()*1000L)/(60*1000)<15L) {
				if (!item.getAttrs().isDir()) {
					ls.add(item.getFilename())
					sftpChannel.rm(path + "/" + item.getFilename());
				} else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
					try {
						sftpChannel.rmdir(path + "/" + item.getFilename());
						ls.add(item.getFilename())
					} catch (Exception e) {
						folderDelete(sftpChannel, path + "/" + item.getFilename(), ls)
					}
				}
			}
		}
	}