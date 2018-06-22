/*
 * xusb: Generic USB test program
 * Copyright © 2009-2012 Pete Batard <pete@akeo.ie>
 * Contributions to Mass Storage by Alan Stern.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

#include "libusb.h"
#include "common.h"

#define LOG_TAG "XUSB"

#if defined(_WIN32)
#define msleep(msecs) Sleep(msecs)
#else

#include <time.h>
#include <jni.h>

#define msleep(msecs) nanosleep(&(struct timespec){msecs / 1000, (msecs * 1000000) % 1000000000UL}, NULL);
#endif

#if defined(_MSC_VER)
#define snprintf _snprintf
#define putenv _putenv
#endif

#if !defined(bool)
#define bool int
#endif
#if !defined(true)
#define true (1 == 1)
#endif
#if !defined(false)
#define false (!true)
#endif

// Future versions of libusb will use usb_interface instead of interface
// in libusb_config_descriptor => catter for that
#define usb_interface interface

// Global variables
static bool binary_dump = false;
static bool extra_info = false;
static bool force_device_request = false;    // For WCID descriptor queries
static const char *binary_name = NULL;

#define ERR_EXIT(errcode) do { LOGE("   %s\n", libusb_strerror((enum libusb_error)errcode)); return -1; } while (0)
#define CALL_CHECK(fcall) do { int _r=fcall; if (_r < 0) ERR_EXIT(_r); } while (0)
#define CALL_CHECK_CLOSE(fcall, hdl) do { int _r=fcall; if (_r < 0) { libusb_close(hdl); ERR_EXIT(_r); } } while (0)
#define B(x) (((x)!=0)?1:0)
#define be_to_int32(buf) (((buf)[0]<<24)|((buf)[1]<<16)|((buf)[2]<<8)|(buf)[3])

#define RETRY_MAX                     5
#define REQUEST_SENSE_LENGTH          0x12
#define INQUIRY_LENGTH                0x24
#define READ_CAPACITY_LENGTH          0x08

// HID Class-Specific Requests values. See section 7.2 of the HID specifications
#define HID_GET_REPORT                0x01
#define HID_GET_IDLE                  0x02
#define HID_GET_PROTOCOL              0x03
#define HID_SET_REPORT                0x09
#define HID_SET_IDLE                  0x0A
#define HID_SET_PROTOCOL              0x0B
#define HID_REPORT_TYPE_INPUT         0x01
#define HID_REPORT_TYPE_OUTPUT        0x02
#define HID_REPORT_TYPE_FEATURE       0x03

// Mass Storage Requests values. See section 3 of the Bulk-Only Mass Storage Class specifications
#define BOMS_RESET                    0xFF
#define BOMS_GET_MAX_LUN              0xFE

// Microsoft OS Descriptor
#define MS_OS_DESC_STRING_INDEX        0xEE
#define MS_OS_DESC_STRING_LENGTH    0x12
#define MS_OS_DESC_VENDOR_CODE_OFFSET    0x10
static const uint8_t ms_os_desc_string[] = {
        MS_OS_DESC_STRING_LENGTH,
        LIBUSB_DT_STRING,
        'M', 0, 'S', 0, 'F', 0, 'T', 0, '1', 0, '0', 0, '0', 0,
};

// Section 5.1: Command Block Wrapper (CBW)
struct command_block_wrapper {
    uint8_t dCBWSignature[4];
    uint32_t dCBWTag;
    uint32_t dCBWDataTransferLength;
    uint8_t bmCBWFlags;
    uint8_t bCBWLUN;
    uint8_t bCBWCBLength;
    uint8_t CBWCB[16];
};

// Section 5.2: Command Status Wrapper (CSW)
struct command_status_wrapper {
    uint8_t dCSWSignature[4];
    uint32_t dCSWTag;
    uint32_t dCSWDataResidue;
    uint8_t bCSWStatus;
};

static const uint8_t cdb_length[256] = {
//	 0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
        06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06,  //  0
        06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06, 06,  //  1
        10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,  //  2
        10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,  //  3
        10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,  //  4
        10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,  //  5
        00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,  //  6
        00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,  //  7
        16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,  //  8
        16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,  //  9
        12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,  //  A
        12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,  //  B
        00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,  //  C
        00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,  //  D
        00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,  //  E
        00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,  //  F
};

static void display_buffer_hex(unsigned char *buffer, unsigned size) {
    unsigned i, j, k;
    char *message = malloc(4096  * sizeof(char));
    int offset = 0;
    for (i = 0; i < size; i += 16) {
        offset += snprintf(message, 11, "\n  %08x  ", i);
        for (j = 0, k = 0; k < 16; j++, k++) {
            if (i + j < size) {
                offset += snprintf(message + offset, 2, "%02x", buffer[i + j]);
            } else {
                offset += snprintf(message + offset, 1, "  ");
            }
            offset += snprintf(message + offset, 1, " ");
        }
        offset += snprintf(message + offset, 1, " ");
        for (j = 0, k = 0; k < 16; j++, k++) {
            if (i + j < size) {
                if ((buffer[i + j] < 32) || (buffer[i + j] > 126)) {
                    offset += snprintf(message + offset, 1, ".");
                } else {
                    offset += snprintf(message + offset, 1, "%c", buffer[i + j]);
                }
            }
        }
    }
    snprintf(message + offset, 1, "\n");
    LOGD("%s", message);
    free(message);
}

static char *uuid_to_string(const uint8_t *uuid) {
    static char uuid_string[40];
    if (uuid == NULL) return NULL;
    snprintf(uuid_string, sizeof(uuid_string),
             "{%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x}",
             uuid[0], uuid[1], uuid[2], uuid[3], uuid[4], uuid[5], uuid[6], uuid[7],
             uuid[8], uuid[9], uuid[10], uuid[11], uuid[12], uuid[13], uuid[14], uuid[15]);
    return uuid_string;
}

static int send_mass_storage_command(libusb_device_handle *handle, uint8_t endpoint, uint8_t lun,
                                     uint8_t *cdb, uint8_t direction, int data_length, uint32_t *ret_tag) {
    static uint32_t tag = 1;
    uint8_t cdb_len;
    int i, r, size;
    struct command_block_wrapper cbw;

    if (cdb == NULL) {
        return -1;
    }

    if (endpoint & LIBUSB_ENDPOINT_IN) {
        LOGE("send_mass_storage_command: cannot send command on IN endpoint\n");
        return -1;
    }

    cdb_len = cdb_length[cdb[0]];
    if ((cdb_len == 0) || (cdb_len > sizeof(cbw.CBWCB))) {
        LOGE("send_mass_storage_command: don't know how to handle this command (%02X, length %d)\n",
             cdb[0], cdb_len);
        return -1;
    }

    memset(&cbw, 0, sizeof(cbw));
    cbw.dCBWSignature[0] = 'U';
    cbw.dCBWSignature[1] = 'S';
    cbw.dCBWSignature[2] = 'B';
    cbw.dCBWSignature[3] = 'C';
    *ret_tag = tag;
    cbw.dCBWTag = tag++;
    cbw.dCBWDataTransferLength = data_length;
    cbw.bmCBWFlags = direction;
    cbw.bCBWLUN = lun;
    // Subclass is 1 or 6 => cdb_len
    cbw.bCBWCBLength = cdb_len;
    memcpy(cbw.CBWCB, cdb, cdb_len);

    i = 0;
    do {
        // The transfer length must always be exactly 31 bytes.
        r = libusb_bulk_transfer(handle, endpoint, (unsigned char *) &cbw, 31, &size, 1000);
        if (r == LIBUSB_ERROR_PIPE) {
            libusb_clear_halt(handle, endpoint);
        }
        i++;
    } while ((r == LIBUSB_ERROR_PIPE) && (i < RETRY_MAX));
    if (r != LIBUSB_SUCCESS) {
        LOGE("   send_mass_storage_command: %s\n", libusb_strerror((enum libusb_error) r));
        return -1;
    }

    printf("   sent %d CDB bytes\n", cdb_len);
    return 0;
}

static int get_mass_storage_status(libusb_device_handle *handle, uint8_t endpoint, uint32_t expected_tag) {
    int i, r, size;
    struct command_status_wrapper csw;

    // The device is allowed to STALL this transfer. If it does, you have to
    // clear the stall and try again.
    i = 0;
    do {
        r = libusb_bulk_transfer(handle, endpoint, (unsigned char *) &csw, 13, &size, 1000);
        if (r == LIBUSB_ERROR_PIPE) {
            libusb_clear_halt(handle, endpoint);
        }
        i++;
    } while ((r == LIBUSB_ERROR_PIPE) && (i < RETRY_MAX));
    if (r != LIBUSB_SUCCESS) {
        LOGE("   get_mass_storage_status: %s\n", libusb_strerror((enum libusb_error) r));
        return -1;
    }
    if (size != 13) {
        LOGE("   get_mass_storage_status: received %d bytes (expected 13)\n", size);
        return -1;
    }
    if (csw.dCSWTag != expected_tag) {
        LOGE("   get_mass_storage_status: mismatched tags (expected %08X, received %08X)\n",
             expected_tag, csw.dCSWTag);
        return -1;
    }
    // For this test, we ignore the dCSWSignature check for validity...
    printf("   Mass Storage Status: %02X (%s)\n", csw.bCSWStatus, csw.bCSWStatus ? "FAILED" : "Success");
    if (csw.dCSWTag != expected_tag)
        return -1;
    if (csw.bCSWStatus) {
        // REQUEST SENSE is appropriate only if bCSWStatus is 1, meaning that the
        // command failed somehow.  Larger values (2 in particular) mean that
        // the command couldn't be understood.
        if (csw.bCSWStatus == 1)
            return -2;    // request Get Sense
        else
            return -1;
    }

    // In theory we also should check dCSWDataResidue.  But lots of devices
    // set it wrongly.
    return 0;
}

static void get_sense(libusb_device_handle *handle, uint8_t endpoint_in, uint8_t endpoint_out) {
    uint8_t cdb[16];    // SCSI Command Descriptor Block
    uint8_t sense[18];
    uint32_t expected_tag;
    int size;
    int rc;

    // Request Sense
    LOGD("Request Sense:\n");
    memset(sense, 0, sizeof(sense));
    memset(cdb, 0, sizeof(cdb));
    cdb[0] = 0x03;    // Request Sense
    cdb[4] = REQUEST_SENSE_LENGTH;

    send_mass_storage_command(handle, endpoint_out, 0, cdb, LIBUSB_ENDPOINT_IN, REQUEST_SENSE_LENGTH, &expected_tag);
    rc = libusb_bulk_transfer(handle, endpoint_in, (unsigned char *) &sense, REQUEST_SENSE_LENGTH, &size, 1000);
    if (rc < 0) {
        LOGD("libusb_bulk_transfer failed: %s\n", libusb_error_name(rc));
        return;
    }
    LOGD("   received %d bytes\n", size);

    if ((sense[0] != 0x70) && (sense[0] != 0x71)) {
        LOGE("   ERROR No sense data\n");
    } else {
        LOGE("   ERROR Sense: %02X %02X %02X\n", sense[2] & 0x0F, sense[12], sense[13]);
    }
    // Strictly speaking, the get_mass_storage_status() call should come
    // before these LOGE() lines.  If the status is nonzero then we must
    // assume there's no data in the buffer.  For xusb it doesn't matter.
    get_mass_storage_status(handle, endpoint_in, expected_tag);
}

// Mass Storage device to test bulk transfers (non destructive test)
static int test_mass_storage(libusb_device_handle *handle, uint8_t endpoint_in, uint8_t endpoint_out) {
    int r, size;
    uint8_t lun;
    uint32_t expected_tag;
    uint32_t i, max_lba, block_size;
    double device_size;
    uint8_t cdb[16];    // SCSI Command Descriptor Block
    uint8_t buffer[64];
    char vid[9], pid[9], rev[5];
    unsigned char *data;
    FILE *fd;

    LOGD("Reading Max LUN:\n");
    r = libusb_control_transfer(handle, LIBUSB_ENDPOINT_IN | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE,
                                BOMS_GET_MAX_LUN, 0, 0, &lun, 1, 1000);
    // Some devices send a STALL instead of the actual value.
    // In such cases we should set lun to 0.
    if (r == 0) {
        lun = 0;
    } else if (r < 0) {
        LOGE("   Failed: %s", libusb_strerror((enum libusb_error) r));
    }
    LOGD("   Max LUN = %d\n", lun);

    // Send Inquiry
    LOGD("Sending Inquiry:\n");
    memset(buffer, 0, sizeof(buffer));
    memset(cdb, 0, sizeof(cdb));
    cdb[0] = 0x12;    // Inquiry
    cdb[4] = INQUIRY_LENGTH;

    send_mass_storage_command(handle, endpoint_out, lun, cdb, LIBUSB_ENDPOINT_IN, INQUIRY_LENGTH, &expected_tag);
    CALL_CHECK(libusb_bulk_transfer(handle, endpoint_in, (unsigned char *) &buffer, INQUIRY_LENGTH, &size, 1000));
    LOGD("   received %d bytes\n", size);
    // The following strings are not zero terminated
    for (i = 0; i < 8; i++) {
        vid[i] = buffer[8 + i];
        pid[i] = buffer[16 + i];
        rev[i / 2] = buffer[32 + i / 2];    // instead of another loop
    }
    vid[8] = 0;
    pid[8] = 0;
    rev[4] = 0;
    LOGD("   VID:PID:REV \"%8s\":\"%8s\":\"%4s\"\n", vid, pid, rev);
    if (get_mass_storage_status(handle, endpoint_in, expected_tag) == -2) {
        get_sense(handle, endpoint_in, endpoint_out);
    }

    // Read capacity
    LOGD("Reading Capacity:\n");
    memset(buffer, 0, sizeof(buffer));
    memset(cdb, 0, sizeof(cdb));
    cdb[0] = 0x25;    // Read Capacity

    send_mass_storage_command(handle, endpoint_out, lun, cdb, LIBUSB_ENDPOINT_IN, READ_CAPACITY_LENGTH, &expected_tag);
    CALL_CHECK(libusb_bulk_transfer(handle, endpoint_in, (unsigned char *) &buffer, READ_CAPACITY_LENGTH, &size, 1000));
    LOGD("   received %d bytes\n", size);
    max_lba = be_to_int32(&buffer[0]);
    block_size = be_to_int32(&buffer[4]);
    device_size = ((double) (max_lba + 1)) * block_size / (1024 * 1024 * 1024);
    LOGD("   Max LBA: %08X, Block Size: %08X (%.2f GB)\n", max_lba, block_size, device_size);
    if (get_mass_storage_status(handle, endpoint_in, expected_tag) == -2) {
        get_sense(handle, endpoint_in, endpoint_out);
    }

    // coverity[tainted_data]
    data = (unsigned char *) calloc(1, block_size);
    if (data == NULL) {
        LOGE("   unable to allocate data buffer\n");
        return -1;
    }

    // Send Read
    LOGD("Attempting to read %u bytes:\n", block_size);
    memset(cdb, 0, sizeof(cdb));

    cdb[0] = 0x28;    // Read(10)
    cdb[8] = 0x01;    // 1 block

    send_mass_storage_command(handle, endpoint_out, lun, cdb, LIBUSB_ENDPOINT_IN, block_size, &expected_tag);
    libusb_bulk_transfer(handle, endpoint_in, data, block_size, &size, 5000);
    LOGD("   READ: received %d bytes\n", size);
    if (get_mass_storage_status(handle, endpoint_in, expected_tag) == -2) {
        get_sense(handle, endpoint_in, endpoint_out);
    } else {
        for (int i = 0; i < size; i += 128) {
            log_dump(LOG_TAG, data + i, 128, 16);
        }
        if ((binary_dump) && ((fd = fopen(binary_name, "w")) != NULL)) {
            if (fwrite(data, 1, (size_t) size, fd) != (unsigned int) size) {
                LOGE("   unable to write binary data\n");
            }
            fclose(fd);
        }
    }
    free(data);

    return 0;
}

// HID
static int get_hid_record_size(uint8_t *hid_report_descriptor, int size, int type) {
    uint8_t i, j = 0;
    uint8_t offset;
    int record_size[3] = {0, 0, 0};
    int nb_bits = 0, nb_items = 0;
    bool found_record_marker;

    found_record_marker = false;
    for (i = hid_report_descriptor[0] + 1; i < size; i += offset) {
        offset = (hid_report_descriptor[i] & 0x03) + 1;
        if (offset == 4)
            offset = 5;
        switch (hid_report_descriptor[i] & 0xFC) {
            case 0x74:    // bitsize
                nb_bits = hid_report_descriptor[i + 1];
                break;
            case 0x94:    // count
                nb_items = 0;
                for (j = 1; j < offset; j++) {
                    nb_items = ((uint32_t) hid_report_descriptor[i + j]) << (8 * (j - 1));
                }
                break;
            case 0x80:    // input
                found_record_marker = true;
                j = 0;
                break;
            case 0x90:    // output
                found_record_marker = true;
                j = 1;
                break;
            case 0xb0:    // feature
                found_record_marker = true;
                j = 2;
                break;
            case 0xC0:    // end of collection
                nb_items = 0;
                nb_bits = 0;
                break;
            default:
                continue;
        }
        if (found_record_marker) {
            found_record_marker = false;
            record_size[j] += nb_items * nb_bits;
        }
    }
    if ((type < HID_REPORT_TYPE_INPUT) || (type > HID_REPORT_TYPE_FEATURE)) {
        return 0;
    } else {
        return (record_size[type - HID_REPORT_TYPE_INPUT] + 7) / 8;
    }
}

static int test_hid(libusb_device_handle *handle, uint8_t endpoint_in) {
    int r, size, descriptor_size;
    uint8_t hid_report_descriptor[256];
    uint8_t *report_buffer;
    FILE *fd;

    LOGD("\nReading HID Report Descriptors:\n");
    descriptor_size = libusb_control_transfer(handle, LIBUSB_ENDPOINT_IN | LIBUSB_REQUEST_TYPE_STANDARD |
                                                      LIBUSB_RECIPIENT_INTERFACE,
                                              LIBUSB_REQUEST_GET_DESCRIPTOR, LIBUSB_DT_REPORT << 8, 0,
                                              hid_report_descriptor, sizeof(hid_report_descriptor), 1000);
    if (descriptor_size < 0) {
        LOGD("   Failed\n");
        return -1;
    }
    display_buffer_hex(hid_report_descriptor, descriptor_size);
    if ((binary_dump) && ((fd = fopen(binary_name, "w")) != NULL)) {
        if (fwrite(hid_report_descriptor, 1, descriptor_size, fd) != descriptor_size) {
            LOGD("   Error writing descriptor to file\n");
        }
        fclose(fd);
    }

    size = get_hid_record_size(hid_report_descriptor, descriptor_size, HID_REPORT_TYPE_FEATURE);
    if (size <= 0) {
        LOGD("\nSkipping Feature Report readout (None detected)\n");
    } else {
        report_buffer = (uint8_t *) calloc(size, 1);
        if (report_buffer == NULL) {
            return -1;
        }

        LOGD("\nReading Feature Report (length %d)...\n", size);
        r = libusb_control_transfer(handle, LIBUSB_ENDPOINT_IN | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE,
                                    HID_GET_REPORT, (HID_REPORT_TYPE_FEATURE << 8) | 0, 0, report_buffer,
                                    (uint16_t) size, 5000);
        if (r >= 0) {
            display_buffer_hex(report_buffer, size);
        } else {
            switch (r) {
                case LIBUSB_ERROR_NOT_FOUND:
                    LOGD("   No Feature Report available for this device\n");
                    break;
                case LIBUSB_ERROR_PIPE:
                    LOGD("   Detected stall - resetting pipe...\n");
                    libusb_clear_halt(handle, 0);
                    break;
                default:
                    LOGD("   Error: %s\n", libusb_strerror((enum libusb_error) r));
                    break;
            }
        }
        free(report_buffer);
    }

    size = get_hid_record_size(hid_report_descriptor, descriptor_size, HID_REPORT_TYPE_INPUT);
    if (size <= 0) {
        LOGD("\nSkipping Input Report readout (None detected)\n");
    } else {
        report_buffer = (uint8_t *) calloc(size, 1);
        if (report_buffer == NULL) {
            return -1;
        }

        LOGD("\nReading Input Report (length %d)...\n", size);
        r = libusb_control_transfer(handle, LIBUSB_ENDPOINT_IN | LIBUSB_REQUEST_TYPE_CLASS | LIBUSB_RECIPIENT_INTERFACE,
                                    HID_GET_REPORT, (HID_REPORT_TYPE_INPUT << 8) | 0x00, 0, report_buffer,
                                    (uint16_t) size, 5000);
        if (r >= 0) {
            display_buffer_hex(report_buffer, size);
        } else {
            switch (r) {
                case LIBUSB_ERROR_TIMEOUT:
                    LOGD("   Timeout! Please make sure you act on the device within the 5 seconds allocated...\n");
                    break;
                case LIBUSB_ERROR_PIPE:
                    LOGD("   Detected stall - resetting pipe...\n");
                    libusb_clear_halt(handle, 0);
                    break;
                default:
                    LOGD("   Error: %s\n", libusb_strerror((enum libusb_error) r));
                    break;
            }
        }

        // Attempt a bulk read from endpoint 0 (this should just return a raw input report)
        LOGD("\nTesting interrupt read using endpoint %02X...\n", endpoint_in);
        r = libusb_interrupt_transfer(handle, endpoint_in, report_buffer, size, &size, 5000);
        if (r >= 0) {
            display_buffer_hex(report_buffer, size);
        } else {
            LOGD("   %s\n", libusb_strerror((enum libusb_error) r));
        }

        free(report_buffer);
    }
    return 0;
}

// Read the MS WinUSB Feature Descriptors, that are used on Windows 8 for automated driver installation
static void read_ms_winsub_feature_descriptors(libusb_device_handle *handle, uint8_t bRequest, int iface_number) {
#define MAX_OS_FD_LENGTH 256
    int i, r;
    uint8_t os_desc[MAX_OS_FD_LENGTH];
    uint32_t length;
    void *le_type_punning_IS_fine;
    struct {
        const char *desc;
        uint8_t recipient;
        uint16_t index;
        uint16_t header_size;
    } os_fd[2] = {
            {"Extended Compat ID",  LIBUSB_RECIPIENT_DEVICE,    0x0004, 0x10},
            {"Extended Properties", LIBUSB_RECIPIENT_INTERFACE, 0x0005, 0x0A}
    };

    if (iface_number < 0) return;
    // WinUSB has a limitation that forces wIndex to the interface number when issuing
    // an Interface Request. To work around that, we can force a Device Request for
    // the Extended Properties, assuming the device answers both equally.
    if (force_device_request)
        os_fd[1].recipient = LIBUSB_RECIPIENT_DEVICE;

    for (i = 0; i < 2; i++) {
        LOGD("\nReading %s OS Feature Descriptor (wIndex = 0x%04d):\n", os_fd[i].desc, os_fd[i].index);

        // Read the header part
        r = libusb_control_transfer(handle,
                                    (uint8_t) (LIBUSB_ENDPOINT_IN | LIBUSB_REQUEST_TYPE_VENDOR | os_fd[i].recipient),
                                    bRequest, (uint16_t) (((iface_number) << 8) | 0x00), os_fd[i].index, os_desc,
                                    os_fd[i].header_size, 1000);
        if (r < os_fd[i].header_size) {
            LOGE("   Failed: %s", (r < 0) ? libusb_strerror((enum libusb_error) r) : "header size is too small");
            return;
        }
        le_type_punning_IS_fine = (void *) os_desc;
        length = *((uint32_t *) le_type_punning_IS_fine);
        if (length > MAX_OS_FD_LENGTH) {
            length = MAX_OS_FD_LENGTH;
        }

        // Read the full feature descriptor
        r = libusb_control_transfer(handle,
                                    (uint8_t) (LIBUSB_ENDPOINT_IN | LIBUSB_REQUEST_TYPE_VENDOR | os_fd[i].recipient),
                                    bRequest, (uint16_t) (((iface_number) << 8) | 0x00), os_fd[i].index, os_desc,
                                    (uint16_t) length, 1000);
        if (r < 0) {
            LOGE("   Failed: %s", libusb_strerror((enum libusb_error) r));
            return;
        } else {
            display_buffer_hex(os_desc, r);
        }
    }
}

static void print_device_cap(struct libusb_bos_dev_capability_descriptor *dev_cap) {
    switch (dev_cap->bDevCapabilityType) {
        case LIBUSB_BT_USB_2_0_EXTENSION: {
            struct libusb_usb_2_0_extension_descriptor *usb_2_0_ext = NULL;
            libusb_get_usb_2_0_extension_descriptor(NULL, dev_cap, &usb_2_0_ext);
            if (usb_2_0_ext) {
                LOGD("    USB 2.0 extension:\n");
                LOGD("      attributes             : %02X\n", usb_2_0_ext->bmAttributes);
                libusb_free_usb_2_0_extension_descriptor(usb_2_0_ext);
            }
            break;
        }
        case LIBUSB_BT_SS_USB_DEVICE_CAPABILITY: {
            struct libusb_ss_usb_device_capability_descriptor *ss_usb_device_cap = NULL;
            libusb_get_ss_usb_device_capability_descriptor(NULL, dev_cap, &ss_usb_device_cap);
            if (ss_usb_device_cap) {
                LOGD("    USB 3.0 capabilities:\n");
                LOGD("      attributes             : %02X\n", ss_usb_device_cap->bmAttributes);
                LOGD("      supported speeds       : %04X\n", ss_usb_device_cap->wSpeedSupported);
                LOGD("      supported functionality: %02X\n", ss_usb_device_cap->bFunctionalitySupport);
                libusb_free_ss_usb_device_capability_descriptor(ss_usb_device_cap);
            }
            break;
        }
        case LIBUSB_BT_CONTAINER_ID: {
            struct libusb_container_id_descriptor *container_id = NULL;
            libusb_get_container_id_descriptor(NULL, dev_cap, &container_id);
            if (container_id) {
                LOGD("    Container ID:\n      %s\n", uuid_to_string(container_id->ContainerID));
                libusb_free_container_id_descriptor(container_id);
            }
            break;
        }
        default:
            LOGD("    Unknown BOS device capability %02x:\n", dev_cap->bDevCapabilityType);
    }
}

static int test_device(libusb_device_handle *handle) {
    libusb_device *dev;
    uint8_t bus, port_path[8];
    struct libusb_bos_descriptor *bos_desc;
    struct libusb_config_descriptor *conf_desc;
    const struct libusb_endpoint_descriptor *endpoint;
    int i, j, k, r;
    int iface, nb_ifaces, first_iface = -1;
    struct libusb_device_descriptor dev_desc;
    const char *const speed_name[5] = {"Unknown", "1.5 Mbit/s (USB LowSpeed)", "12 Mbit/s (USB FullSpeed)",
                                       "480 Mbit/s (USB HighSpeed)", "5000 Mbit/s (USB SuperSpeed)"};
    char string[128];
    uint8_t string_index[3];    // indexes of the string descriptors
    uint8_t endpoint_in = 0, endpoint_out = 0;    // default IN and OUT endpoints

    if (handle == NULL) {
        LOGE("  Failed.\n");
        return -1;
    }

    dev = libusb_get_device(handle);
    bus = libusb_get_bus_number(dev);
    if (extra_info) {
        r = libusb_get_port_numbers(dev, port_path, sizeof(port_path));
        if (r > 0) {
            LOGD("\nDevice properties:\n");
            LOGD("        bus number: %d\n", bus);
            LOGD("         port path: %d", port_path[0]);
            for (i = 1; i < r; i++) {
                LOGD("->%d", port_path[i]);
            }
            LOGD(" (from root hub)\n");
        }
        r = libusb_get_device_speed(dev);
        if ((r < 0) || (r > 4)) r = 0;
        LOGD("             speed: %s\n", speed_name[r]);
    }

    LOGD("\nReading device descriptor:\n");
    CALL_CHECK_CLOSE(libusb_get_device_descriptor(dev, &dev_desc), handle);
    LOGD("            length: %d\n", dev_desc.bLength);
    LOGD("      device class: %d\n", dev_desc.bDeviceClass);
    LOGD("               S/N: %d\n", dev_desc.iSerialNumber);
    LOGD("           VID:PID: %04X:%04X\n", dev_desc.idVendor, dev_desc.idProduct);
    LOGD("         bcdDevice: %04X\n", dev_desc.bcdDevice);
    LOGD("   iMan:iProd:iSer: %d:%d:%d\n", dev_desc.iManufacturer, dev_desc.iProduct, dev_desc.iSerialNumber);
    LOGD("          nb confs: %d\n", dev_desc.bNumConfigurations);
    // Copy the string descriptors for easier parsing
    string_index[0] = dev_desc.iManufacturer;
    string_index[1] = dev_desc.iProduct;
    string_index[2] = dev_desc.iSerialNumber;

    LOGD("\nReading BOS descriptor: ");
    if (libusb_get_bos_descriptor(handle, &bos_desc) == LIBUSB_SUCCESS) {
        LOGD("%d caps\n", bos_desc->bNumDeviceCaps);
        for (i = 0; i < bos_desc->bNumDeviceCaps; i++)
            print_device_cap(bos_desc->dev_capability[i]);
        libusb_free_bos_descriptor(bos_desc);
    } else {
        LOGD("no descriptor\n");
    }

    LOGD("\nReading first configuration descriptor:\n");
    CALL_CHECK_CLOSE(libusb_get_config_descriptor(dev, 0, &conf_desc), handle);
    nb_ifaces = conf_desc->bNumInterfaces;
    LOGD("             nb interfaces: %d\n", nb_ifaces);
    if (nb_ifaces > 0)
        first_iface = conf_desc->usb_interface[0].altsetting[0].bInterfaceNumber;
    for (i = 0; i < nb_ifaces; i++) {
        LOGD("              interface[%d]: id = %d\n", i,
               conf_desc->usb_interface[i].altsetting[0].bInterfaceNumber);
        for (j = 0; j < conf_desc->usb_interface[i].num_altsetting; j++) {
            LOGD("interface[%d].altsetting[%d]: num endpoints = %d\n",
                   i, j, conf_desc->usb_interface[i].altsetting[j].bNumEndpoints);
            LOGD("   Class.SubClass.Protocol: %02X.%02X.%02X\n",
                   conf_desc->usb_interface[i].altsetting[j].bInterfaceClass,
                   conf_desc->usb_interface[i].altsetting[j].bInterfaceSubClass,
                   conf_desc->usb_interface[i].altsetting[j].bInterfaceProtocol);
            if ((conf_desc->usb_interface[i].altsetting[j].bInterfaceClass == LIBUSB_CLASS_MASS_STORAGE)
                && ((conf_desc->usb_interface[i].altsetting[j].bInterfaceSubClass == 0x01)
                    || (conf_desc->usb_interface[i].altsetting[j].bInterfaceSubClass == 0x06))
                && (conf_desc->usb_interface[i].altsetting[j].bInterfaceProtocol == 0x50)) {
                // Mass storage devices that can use basic SCSI commands
            }
            for (k = 0; k < conf_desc->usb_interface[i].altsetting[j].bNumEndpoints; k++) {
                struct libusb_ss_endpoint_companion_descriptor *ep_comp = NULL;
                endpoint = &conf_desc->usb_interface[i].altsetting[j].endpoint[k];
                LOGD("       endpoint[%d].address: %02X\n", k, endpoint->bEndpointAddress);
                // Use the first interrupt or bulk IN/OUT endpoints as default for testing
                if ((endpoint->bmAttributes & LIBUSB_TRANSFER_TYPE_MASK) &
                    (LIBUSB_TRANSFER_TYPE_BULK | LIBUSB_TRANSFER_TYPE_INTERRUPT)) {
                    if (endpoint->bEndpointAddress & LIBUSB_ENDPOINT_IN) {
                        if (!endpoint_in)
                            endpoint_in = endpoint->bEndpointAddress;
                    } else {
                        if (!endpoint_out)
                            endpoint_out = endpoint->bEndpointAddress;
                    }
                }
                LOGD("           max packet size: %04X\n", endpoint->wMaxPacketSize);
                LOGD("          polling interval: %02X\n", endpoint->bInterval);
                libusb_get_ss_endpoint_companion_descriptor(NULL, endpoint, &ep_comp);
                if (ep_comp) {
                    LOGD("                 max burst: %02X   (USB 3.0)\n", ep_comp->bMaxBurst);
                    LOGD("        bytes per interval: %04X (USB 3.0)\n", ep_comp->wBytesPerInterval);
                    libusb_free_ss_endpoint_companion_descriptor(ep_comp);
                }
            }
        }
    }
    libusb_free_config_descriptor(conf_desc);

    libusb_set_auto_detach_kernel_driver(handle, 1);
    for (iface = 0; iface < nb_ifaces; iface++) {
        LOGD("\nClaiming interface %d...\n", iface);
        r = libusb_claim_interface(handle, iface);
        if (r != LIBUSB_SUCCESS) {
            LOGE("   Failed.\n");
        }
    }

    LOGD("\nReading string descriptors:\n");
    for (i = 0; i < 3; i++) {
        if (string_index[i] == 0) {
            continue;
        }
        if (libusb_get_string_descriptor_ascii(handle, string_index[i], (unsigned char *) string, sizeof(string)) > 0) {
            LOGD("   String (0x%02X): \"%s\"\n", string_index[i], string);
        }
    }
    // Read the OS String Descriptor
    r = libusb_get_string_descriptor(handle, MS_OS_DESC_STRING_INDEX, 0, (unsigned char *) string,
                                     MS_OS_DESC_STRING_LENGTH);
    if (r == MS_OS_DESC_STRING_LENGTH && memcmp(ms_os_desc_string, string, sizeof(ms_os_desc_string)) == 0) {
        // If this is a Microsoft OS String Descriptor,
        // attempt to read the WinUSB extended Feature Descriptors
        read_ms_winsub_feature_descriptors(handle, string[MS_OS_DESC_VENDOR_CODE_OFFSET], first_iface);
    }

    CALL_CHECK_CLOSE(test_mass_storage(handle, endpoint_in, endpoint_out), handle);

    LOGD("\n");
    for (iface = 0; iface < nb_ifaces; iface++) {
        LOGD("Releasing interface %d...\n", iface);
        libusb_release_interface(handle, iface);
    }

    LOGD("Closing device...\n");
    libusb_close(handle);

    return 0;
}

JNIEXPORT void JNICALL
Java_com_jwoolston_android_libusb_UsbDeviceConnection_runLibUsbTest(JNIEnv *env, jobject instance, jobject device) {
    struct libusb_device_handle *deviceHandle = (struct libusb_device_handle *) (*env)->GetDirectBufferAddress(env,
                                                                                                               device);
    test_device(deviceHandle);
}
