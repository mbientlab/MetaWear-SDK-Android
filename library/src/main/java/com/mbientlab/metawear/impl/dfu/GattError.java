/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/

/*
 * NORDIC SEMICONDUCTOR EXAMPLE CODE AND LICENSE AGREEMENT
 *
 * You are receiving this document because you have obtained example code ("Software") 
 * from Nordic Semiconductor ASA * ("Licensor"). The Software is protected by copyright 
 * laws and international treaties. All intellectual property rights related to the 
 * Software is the property of the Licensor. This document is a license agreement governing 
 * your rights and obligations regarding usage of the Software. Any variation to the terms 
 * of this Agreement shall only be valid if made in writing by the Licensor.
 * 
 * == Scope of license rights ==
 * 
 * You are hereby granted a limited, non-exclusive, perpetual right to use and modify the 
 * Software in order to create your own software. You are entitled to distribute the 
 * Software in original or modified form as part of your own software.
 *
 * If distributing your software in source code form, a copy of this license document shall 
 * follow with the distribution.
 *   
 * The Licensor can at any time terminate your rights under this license agreement.
 * 
 * == Restrictions on license rights ==
 * 
 * You are not allowed to distribute the Software on its own, without incorporating it into 
 * your own software.  
 * 
 * You are not allowed to remove, alter or destroy any proprietary, 
 * trademark or copyright markings or notices placed upon or contained with the Software.
 *     
 * You shall not use Licensor's name or trademarks without Licensor's prior consent.
 * 
 * == Disclaimer of warranties and limitation of liability ==
 * 
 * YOU EXPRESSLY ACKNOWLEDGE AND AGREE THAT USE OF THE SOFTWARE IS AT YOUR OWN RISK AND THAT THE 
 * SOFTWARE IS PROVIDED *AS IS" WITHOUT ANY WARRANTIES OR CONDITIONS WHATSOEVER. NORDIC SEMICONDUCTOR ASA 
 * DOES NOT WARRANT THAT THE FUNCTIONS OF THE SOFTWARE WILL MEET YOUR REQUIREMENTS OR THAT THE 
 * OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR FREE. YOU ASSUME RESPONSIBILITY FOR 
 * SELECTING THE SOFTWARE TO ACHIEVE YOUR INTENDED RESULTS, AND FOR THE *USE AND THE RESULTS 
 * OBTAINED FROM THE SOFTWARE.
 * 
 * NORDIC SEMICONDUCTOR ASA DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO WARRANTIES RELATED TO: NON-INFRINGEMENT, LACK OF VIRUSES, ACCURACY OR COMPLETENESS OF RESPONSES 
 * OR RESULTS, IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL OR 
 * CONSEQUENTIAL DAMAGES OR FOR ANY DAMAGES WHATSOEVER (INCLUDING BUT NOT LIMITED TO DAMAGES FOR 
 * LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, PERSONAL INJURY, 
 * LOSS OF PRIVACY OR OTHER PECUNIARY OR OTHER LOSS WHATSOEVER) ARISING OUT OF USE OR INABILITY TO 
 * USE THE SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * REGARDLESS OF THE FORM OF ACTION, NORDIC SEMICONDUCTOR ASA AGGREGATE LIABILITY ARISING OUT OF 
 * OR RELATED TO THIS AGREEMENT SHALL NOT EXCEED THE TOTAL AMOUNT PAYABLE BY YOU UNDER THIS AGREEMENT. 
 * THE FOREGOING LIMITATIONS, EXCLUSIONS AND DISCLAIMERS SHALL APPLY TO THE MAXIMUM EXTENT ALLOWED BY 
 * APPLICABLE LAW.
 * 
 * == Dispute resolution and legal venue ==
 * 
 * Any and all disputes arising out of the rights and obligations in this license agreement shall be 
 * submitted to ordinary court proceedings. You accept the Oslo City Court as legal venue under this agreement.
 * 
 * This license agreement shall be governed by Norwegian law.
 * 
 * == Contact information ==
 * 
 * All requests regarding the Software or the API shall be directed to: 
 * Nordic Semiconductor ASA, P.O. Box 436, Skøyen, 0213 Oslo, Norway.
 * 
 * http://www.nordicsemi.com/eng/About-us/Contact-us
 */
package com.mbientlab.metawear.impl.dfu;

public class GattError {

	public static String parse(final int error) {
		switch (error) {
		case 0x0001:
			return "GATT INVALID HANDLE";
		case 0x0002:
			return "GATT READ NOT PERMIT";
		case 0x0003:
			return "GATT WRITE NOT PERMIT";
		case 0x0004:
			return "GATT INVALID PDU";
		case 0x0005:
			return "GATT INSUF AUTHENTICATION";
		case 0x0006:
			return "GATT REQ NOT SUPPORTED";
		case 0x0007:
			return "GATT INVALID OFFSET";
		case 0x0008:
			return "GATT INSUF AUTHORIZATION";
		case 0x0009:
			return "GATT PREPARE Q FULL";
		case 0x000a:
			return "GATT NOT FOUND";
		case 0x000b:
			return "GATT NOT LONG";
		case 0x000c:
			return "GATT INSUF KEY SIZE";
		case 0x000d:
			return "GATT INVALID ATTR LEN";
		case 0x000e:
			return "GATT ERR UNLIKELY";
		case 0x000f:
			return "GATT INSUF ENCRYPTION";
		case 0x0010:
			return "GATT UNSUPPORT GRP TYPE";
		case 0x0011:
			return "GATT INSUF RESOURCE";
		case 0x0087:
			return "GATT ILLEGAL PARAMETER";
		case 0x0080:
			return "GATT NO RESOURCES";
		case 0x0081:
			return "GATT INTERNAL ERROR";
		case 0x0082:
			return "GATT WRONG STATE";
		case 0x0083:
			return "GATT DB FULL";
		case 0x0084:
			return "GATT BUSY";
		case 0x0085:
			return "GATT ERROR";
		case 0x0086:
			return "GATT CMD STARTED";
		case 0x0088:
			return "GATT PENDING";
		case 0x0089:
			return "GATT AUTH FAIL";
		case 0x008a:
			return "GATT MORE";
		case 0x008b:
			return "GATT INVALID CFG";
		case 0x008c:
			return "GATT SERVICE STARTED";
		case 0x008d:
			return "GATT ENCRYPED NO MITM";
		case 0x008e:
			return "GATT NOT ENCRYPTED";
		case 0x00FF:
			return "DFU SERVICE DSCOVERY NOT STARTED";
		case DfuService.ERROR_DEVICE_DISCONNECTED:
			return "DFU DEVICE DISCONNECTED";
		case DfuService.ERROR_FILE_CLOSED:
			return "DFU FILE CLOSED";
		case DfuService.ERROR_FILE_INVALID:
			return "DFU NOT A VALID HEX FILE";
		case DfuService.ERROR_FILE_IO_EXCEPTION:
			return "DFU IO EXCEPTION";
		case DfuService.ERROR_FILE_NOT_FOUND:
			return "DFU FILE NOT FOUND";
		case DfuService.ERROR_SERVICE_DISCOVERY_NOT_STARTED:
			return "DFU ERROR WHILE SERVICE DISCOVERY";
		case DfuService.ERROR_SERVICE_NOT_FOUND:
			return "DFU SERVICE NOT FOUND";
		case DfuService.ERROR_CHARACTERISTICS_NOT_FOUND:
			return "DFU CHARACTERISTICS NOT FOUND";
		default:
			if ((DfuService.ERROR_REMOTE_MASK & error) > 0) {
				switch (error & (~DfuService.ERROR_REMOTE_MASK)) {
				case DfuService.DFU_STATUS_INVALID_STATE:
					return "REMOTE DFU INVALID STATE";
				case DfuService.DFU_STATUS_NOT_SUPPORTED:
					return "REMOTE DFU NOT SUPPORTED";
				case DfuService.DFU_STATUS_DATA_SIZE_EXCEEDS_LIMIT:
					return "REMOTE DFU DATA SIZE EXCEEDS LIMIT";
				case DfuService.DFU_STATUS_CRC_ERROR:
					return "REMOTE DFU INVALID CRC ERROR";
				case DfuService.DFU_STATUS_OPERATION_FAILED:
					return "REMOTE DFU OPERATION FAILED";
				}
			}
			return String.format("UNKNOWN (0x%02X)", error & 0xFF);
		}
	}
}
