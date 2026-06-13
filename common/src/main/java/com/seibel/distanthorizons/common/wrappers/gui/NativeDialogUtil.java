package com.seibel.distanthorizons.common.wrappers.gui;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

/**
 * Should be used instead of the direct call to {@link TinyFileDialogs}
 * so we can run additional validation and/or string cleanup.
 * Otherwise, we may get error messages back. <br><br>
 * 
 * source:
 * https://sourceforge.net/projects/tinyfiledialogs/
 * 
 * @see TinyFileDialogs
 */
public class NativeDialogUtil
{
	/**
	 * @param dialogType    the dialog type. One of:<br><table><tr><td>"ok"</td><td>"okcancel"</td><td>"yesno"</td><td>"yesnocancel"</td></tr></table>
	 * @param iconType      the icon type. One of:<br><table><tr><td>"info"</td><td>"warning"</td><td>"error"</td><td>"question"</td></tr></table>
	 */
	public static void showDialog(String title, String message, String dialogType, String iconType)
	{
		// Tinyfd doesn't support the following characters, attempting to display them will cause the message
		// to be replaced with an error message
		String unsafeCharsRegex = "['\"`]";
		
		title = title.replaceAll(unsafeCharsRegex, "");
		message = message.replaceAll(unsafeCharsRegex, "");
		
		#if MC_VER > MC_1_7_10 //MC_VER <= MC_1_21_11
		TinyFileDialogs.tinyfd_messageBox(title, message, dialogType, iconType, false);
		#else
		// https://mfbridge.github.io/tinyfiledialogs/reference/messageBox.html
		TinyFileDialogs.tinyfd_messageBox(title, message, dialogType, iconType, 1 /* ok/yes */);
		#endif
	}
	
}
