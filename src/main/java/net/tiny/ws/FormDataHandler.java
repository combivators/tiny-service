package net.tiny.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;

public abstract class FormDataHandler extends BaseWebService {

    public static enum PartType {
        TEXT, FILE
    }

    public static class MultiPart {
        public PartType type;
        public String contentType;
        public String name;
        public String filename;
        public String value;
        public byte[] bytes;

        @Override
        public String toString() {
            int len = 0;
            if (bytes != null) {
                len = bytes.length;
            }
            return String.format("[%s] '%s' = '%s' (bytes:%d)", type.name(), name, value!=null ? value :"", len);
        }
    }

    protected abstract void handle(HttpExchange httpExchange,List<MultiPart> parts) throws IOException;

    @Override
    protected String getAllowedMethods() {
        return "POST";
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange httpExchange) throws IOException {
        final RequestHelper request = new RequestHelper(httpExchange);
        if (request.hasMultipart()) {
            //found form data
            final String boundary = request.boundary();
            // as of rfc7578 - prepend "--"
            final byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
            final byte[] payload = readContents(httpExchange.getRequestBody());
            final List<MultiPart> list = parse(payload, boundaryBytes);
            handle(httpExchange, list);
        } else {
            //if no form data is present, still call handle method
            handle(httpExchange, null);
        }
    }


    private byte[] readContents(InputStream requestStream) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[100000];
            int bytesRead=0;
            while ((bytesRead = requestStream.read(buf)) != -1){
                bos.write(buf, 0, bytesRead);
            }
            requestStream.close();
            bos.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while decoding http input stream", e);
        }
        return bos.toByteArray();
    }

    /**
     * Parse request form data, return form data parts
     * @param payload
     * @param boundaryBytes
     * @return multi part list
     */
    public static List<MultiPart> parse(final byte[] payload, final byte[] boundaryBytes) {
        final ArrayList<MultiPart> list = new ArrayList<>();
        List<Integer> offsets = FormDataHandler.searchBytes(payload, boundaryBytes, 0, payload.length-1);
        for (int idx=0; idx<offsets.size(); idx++) {
            int startPart = offsets.get(idx);
            int endPart = payload.length;
            if (idx<offsets.size()-1){
                endPart = offsets.get(idx+1);
            }
            byte[] part = Arrays.copyOfRange(payload,startPart,endPart);
            //look for header
            int headerEnd = FormDataHandler.indexOf(part,"\r\n\r\n".getBytes(Charset.forName("UTF-8")),0,part.length-1);
            if (headerEnd>0) {
                MultiPart p = new MultiPart();
                byte[] head = Arrays.copyOfRange(part, 0, headerEnd);
                String header = new String(head);
                // extract name from header
                int nameIndex = header.indexOf("\r\nContent-Disposition: form-data; name=");
                if (nameIndex >= 0) {
                    int startMarker = nameIndex + 39;
                    //check for extra filename field
                    int fileNameStart = header.indexOf("; filename=");
                    if (fileNameStart >= 0) {
                        String filename = header.substring(fileNameStart + 11, header.indexOf("\r\n", fileNameStart));
                        p.filename = filename.replace('"', ' ').replace('\'', ' ').trim();
                        p.name = header.substring(startMarker, fileNameStart).replace('"', ' ').replace('\'', ' ').trim();
                        p.type = PartType.FILE;
                    } else {
                        int endMarker = header.indexOf("\r\n", startMarker);
                        if (endMarker == -1)
                            endMarker = header.length();
                        p.name = header.substring(startMarker, endMarker).replace('"', ' ').replace('\'', ' ').trim();
                        p.type = PartType.TEXT;
                    }
                } else {
                    // skip entry if no name is found
                    continue;
                }
                // extract content type from header
                int typeIndex = header.indexOf("\r\nContent-Type:");
                if (typeIndex >= 0) {
                    int startMarker = typeIndex + 15;
                    int endMarker = header.indexOf("\r\n", startMarker);
                    if (endMarker == -1)
                        endMarker = header.length();
                    p.contentType = header.substring(startMarker, endMarker).trim();
                }

                //handle content
                if (p.type == PartType.TEXT) {
                    //extract text value
                    byte[] body = Arrays.copyOfRange(part, headerEnd + 4, part.length-2);
                    p.value = new String(body);
                } else {
                    //must be a file upload
                    p.bytes = Arrays.copyOfRange(part, headerEnd + 4, part.length-2);
                }
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Search bytes in byte array returns indexes within this byte-array of all
     * occurrences of the specified(search bytes) byte array in the specified
     * range
     * borrowed from https://github.com/riversun/finbin/blob/master/src/main/java/org/riversun/finbin/BinarySearcher.java
     *
     * @param source
     * @param target
     * @param start
     * @param end
     * @return result index list
     */
    protected static List<Integer> searchBytes(byte[] source, byte[] target, int start, int end) {
        final int destSize = target.length;
        final List<Integer> positionIndexList = new ArrayList<Integer>();
        int cursor = start;
        while (cursor < end + 1) {
            int index = indexOf(source, target, cursor, end);
            if (index >= 0) {
                positionIndexList.add(index);
                cursor = index + destSize;
            } else {
                cursor++;
            }
        }
        return positionIndexList;
    }

    /**
     * Returns the index within this byte-array of the first occurrence of the
     * specified(search bytes) byte array.<br>
     * Starting the search at the specified index, and end at the specified index.
     *
     * @param source
     * @param target
     * @param start
     * @param end
     * @return
     */
    static int indexOf(byte[] source, byte[] target, int start, int end) {
        if (target.length == 0 || (end - start + 1) < target.length) {
            return -1;
        }
        int maxScanStartPosIdx = source.length - target.length;
        final int loopEndIdx;
        if (end < maxScanStartPosIdx) {
            loopEndIdx = end;
        } else {
            loopEndIdx = maxScanStartPosIdx;
        }

        for(int i = start; i <= loopEndIdx; i++) {
            boolean found = true;
            for(int j = 0; j < target.length; j++) {
               if (source[i+j] != target[j]) {
                   found = false;
                   break;
               }
            }
            if (found) return i;
         }
       return -1;
    }
}