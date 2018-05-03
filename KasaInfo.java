import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class KasaInfo {

	/**
	 * The URL used to make HTTP post request
	 */
	private final static String URL = "https://wap.tplinkcloud.com";

	/**
	 * The Kasa account token
	 */
	private String token;

	/**
	 * Number of lights/devices associated with the Kasa account
	 */
	private int numOfLights;

	/**
	 * The multi-dimentional array containing the info about the lights/devices
	 */
	private String[][] lights;

	/**
	 * The credentials to the Kasa account
	 */
	private String password, userName;
	
	/**
	 * Getter for the info about the lights/devices
	 * 
	 * @return a multi-dimentional array
	 */
	public String[][] getLights() {
		return lights;
	}
	
	/**
	 * Setter for the password of the Kasa account
	 * 
	 * @param password
	 *            the password of the account
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * Setter for the user name of the Kasa account
	 * 
	 * @param userName
	 *            the user name of the account
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	/**
	 * Constructor of the class KasaInfo
	 * 
	 * @param userName
	 *            the user name of the account
	 * @param password
	 *            the password of the account
	 */
	public KasaInfo(String password, String userName) {
		this.password = password;
		this.userName = userName;
		
		try {
			getToken();
			getInfo();
		} catch (IOException e) {
			System.out.println("Please have a valid Internet connection and/or access to write to this disk");
		}
	}

	/**
	 * Gets the token of the Kasa account to get info from the account and control
	 * the switches
	 */
	private void getToken() throws IOException {
		String uuid = UUID.randomUUID().toString();

		String message = "{ \"method\": \"login\", \"params\": { \"appType\": \"Kasa_Android\", \"cloudUserName\": \""
				+ userName + "\", \"cloudPassword\": \"" + password + "\", \"terminalUUID\": \"" + uuid + "\"} }";

		String output = postHttpRequest(URL, message);
		token = jsonString(output, "token", output.indexOf("token"), 3, '\"');
	}

	/**
	 * Gets the info from the lights, which includes
	 */
	private void getInfo() throws IOException {
		String output = postHttpRequest(URL + "//?token=" + token, "{\"method\":\"getDeviceList\"}");

		numOfLights = (output.length() - output.replace("alias", "").length()) / 5;

		lights = new String[numOfLights][3];
		int[] indexes = new int[2];

		for (int i = 0; i < numOfLights; i++) {
			indexes[0] = output.indexOf("alias", indexes[0] + 1);
			indexes[1] = output.indexOf("deviceId", indexes[1] + 1);

			lights[i][0] = jsonString(output, "alias", indexes[0], 3, '\"');
			lights[i][2] = jsonString(output, "deviceId", indexes[1], 3, '\"');

			String details = postHttpRequest(URL + "//?token=" + token,
					"{\"method\":\"passthrough\", \"params\": {\"deviceId\": \"" + lights[i][2]
							+ "\", \"requestData\": \"{\\\"system\\\":{\\\"get_sysinfo\\\":null},\\\"emeter\\\":{\\\"get_realtime\\\":null}}\" }}");
			lights[i][1] = jsonString(details, "relay_state", details.indexOf("relay_state"), 3, ',');
		}
	}

	/**
	 * Puts either the device on or off depending on its current state
	 * 
	 * @param device
	 *            the choice of device (start from to 0 to the number of devices you
	 *            have - 1)
	 */
	public void changeState(int device) throws IOException {
		int state = Math.abs(Integer.valueOf(lights[device][1]) - 1);
		lights[device][1] = String.valueOf(state);
		System.out.println(Integer.valueOf(lights[device][1]));
		String message = "{\"method\":\"passthrough\", \"params\": {\"deviceId\":\"" + lights[device][2]
				+ "\", \"requestData\": \"{\\\"system\\\":{\\\"set_relay_state\\\":{\\\"state\\\":" + state
				+ "}}}\" }}";
		postHttpRequest(URL + "//?token=" + token, message);
	}

	/**
	 * Returns the desired value of a JSON object variable
	 * 
	 * @param message
	 *            the JSON message
	 * @param word
	 *            the JSON variable name
	 * @param startIndex
	 *            the index of the JSON variable name
	 * @param offset
	 *            the number of character between the end of the JSON variable name
	 *            and the start of the value
	 * @param finish
	 *            the character to stop at
	 * @return value of JSON variable
	 */
	private String jsonString(String message, String word, int startIndex, int offset, char finish) {
		String token = "";

		for (char i : message.substring(startIndex + word.length() + offset).toCharArray()) {
			if (i == finish) {
				break;
			} else {
				token += i;
			}
		}

		return token;
	}

	/**
	 * Returns the JSON response from the post request
	 * 
	 * @param link
	 *            the URL to make a post request
	 * @param message
	 *            JSON body
	 * @return JSON response
	 */
	private String postHttpRequest(String link, String message) throws IOException {
		URL url = new URL(link);
		URLConnection con = url.openConnection();
		HttpURLConnection http = (HttpURLConnection) con;
		http.setRequestMethod("POST");
		http.setDoOutput(true);

		byte[] out = message.getBytes(StandardCharsets.UTF_8);
		int length = out.length;

		http.setFixedLengthStreamingMode(length);
		http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		http.connect();
		try (OutputStream os = http.getOutputStream()) {
			os.write(out);
		}

		char[] output = new char[http.getInputStream().available()];
		for (int i = 0; i < output.length; i++) {
			output[i] = ((char) http.getInputStream().read());
		}

		return new String(output);
	}

	/**
	 * String representation of the information on the devices/lights associated
	 * with the Kasa account
	 */
	public String toString() {
		String[] rep = new String[numOfLights];

		for (int i = 0; i < numOfLights; i++) {
			rep[i] = Arrays.toString(lights[i]);
		}

		return Arrays.toString(rep);
	}
}
