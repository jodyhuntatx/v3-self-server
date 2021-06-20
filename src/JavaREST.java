import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.lang.InterruptedException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class JavaREST {

  /******************************************************************
   * 			PUBLIC MEMBERS
   *
   * String httpGet(url,authhdr)
   * String httpPost(url,body,authhdr)
   * String httpPatch(url,body,authhdr)
   * String httpDelete(url,authhdr)
   *
   ******************************************************************/

    public static Boolean DEBUG=false;

    // ===============================================================
    // String httpGet() -
    //
    public static String httpGet(String url_string, String auth_header) {
	String output = "";
	try {
	    URL url = new URL(url_string);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("GET");
	    conn.setRequestProperty("Accept", "application/json");
	    conn.setRequestProperty("Authorization", auth_header);

	    switch(conn.getResponseCode()) {
		case 200 :
		    break;

		default :
		    output = null;
		    throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
	    }

	    BufferedReader br = new BufferedReader(new InputStreamReader(
			(conn.getInputStream())));

	    output = br.readLine();
	    String tmp; 
	    while ((tmp = br.readLine()) != null) {
		output = output + System.lineSeparator() + tmp;
	    }

	    conn.disconnect();

	} catch (RuntimeException e) {
	  if(JavaREST.DEBUG) {
	    e.printStackTrace();
	  }
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	if(JavaREST.DEBUG) {
	   System.out.println("JavaREST.httpGet() ========");
	   System.out.println(output);
	}

	return output;

     } // httpGet()


    // ===============================================================
    // String httpPost() -
    //
    public static String httpPost(String url_string, String bodyContent, String auth_header) {
	String output = "";
	try {
	    URL url = new URL(url_string);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setDoOutput(true);
	    conn.setRequestMethod("POST");
	    conn.setRequestProperty("Content-Type", "application/json");
	    conn.setRequestProperty("Authorization", auth_header);

	    OutputStream os = conn.getOutputStream();
	    os.write(bodyContent.getBytes());
	    os.flush();

	    switch(conn.getResponseCode()) {
		case 200 :
		case 201 :
		    break;

		default :
		    output = null;
		    throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
	    }

	    BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

	    String tmp;
	    while ((tmp = br.readLine()) != null) {
			output = output + tmp;
	    }

	    conn.disconnect();

	} catch (RuntimeException e) {
	  if(JavaREST.DEBUG) {
	    e.printStackTrace();
	  }
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	if(JavaREST.DEBUG) {
	   System.out.println("JavaREST.httpPost() ========");
	   System.out.println("Response:");
	   System.out.println(output);
	   System.out.println("============================");
	}

	return output;

     } // httpPost()


    // ===============================================================
    // String httpPatch() - PATCH implemented w/ HttpURLConnection class
    //
    public static String httpPatch(String url_string, String bodyContent, String auth_header) {
	String output = "";
	allowMethods("PATCH");
	try {
	    URL url = new URL(url_string);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setDoOutput(true);
	    conn.setRequestMethod("PATCH");
	    conn.setRequestProperty("Content-Type", "application/json");
	    conn.setRequestProperty("Authorization", auth_header);

	    OutputStream os = conn.getOutputStream();
	    os.write(bodyContent.getBytes());
	    os.flush();

	    switch(conn.getResponseCode()) {
		case 201 :
		    break;
		default :
		    output = null;
		    throw new RuntimeException("Failed : HTTP error code : "
							+ conn.getResponseCode());
	    }

	    BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

	    String tmp;
	    while ((tmp = br.readLine()) != null) {
			output = output + tmp;
	    }

	    conn.disconnect();

	} catch (MalformedURLException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	return output;

     } // httpPatch()

    // ===============================================================
    // Integer httpDelete() - returns response code
    //
    public static Integer httpDelete(String url_string, String auth_header) {
	Integer responseCode=0;
	try {
	    URL url = new URL(url_string);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("DELETE");
	    conn.setRequestProperty("Accept", "application/json");
	    conn.setRequestProperty("Authorization", auth_header);
	    responseCode = conn.getResponseCode();
	    conn.disconnect();
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	return responseCode;

     } // httpDelete()

  /******************************************************************
   * 			PRIVATE MEMBERS
   *
   ******************************************************************/

    // ===============================================================
    private static void allowMethods(String... methods) {
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

            methodsField.setAccessible(true);

            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList(methods));
            String[] newMethods = methodsSet.toArray(new String[0]);

            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // ===============================================================
    // void disableAccessWarnings() - suppresses "Illegal reflection" warnings.
    //
    public static void disableAccessWarnings() {
      try {
         Class unsafeClass = Class.forName("sun.misc.Unsafe");
         Field field = unsafeClass.getDeclaredField("theUnsafe");
         field.setAccessible(true);
         Object unsafe = field.get(null);

         Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
         Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

         Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
         Field loggerField = loggerClass.getDeclaredField("logger");
         Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
         putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
      } catch (Exception ignored) {
         //ignore
      }
   }

} // JavaREST
