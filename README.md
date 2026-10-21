# 🛡️ Leonardo Controller // HID Terminal & Bluetooth Automation

An advanced physical computing project featuring an **Android Jetpack Compose** companion application that communicates wirelessly via an **HC-05 Bluetooth module** to an **Arduino Leonardo**[cite: 5]. 

The system allows you to remotely trigger automated computer workflows (HID keyboard injection) and toggle hardware indicators/relays dynamically from an Android device with a cyberpunk-themed interface[cite: 5].

---

## 🚀 Features

* **Matrix Cyberpunk UI**: Built with Jetpack Compose featuring custom canvas animations, glowing gradients, and dynamic structural components.
* **Wireless HC-05 Link**: Real-time serial communication running at `9600` baud.
* **Automated HID Payloads**:
  * **Workflow 1**: Triggers a secure PowerShell GUI notification/system validation message.
  * **Workflow 2**: Downloads an image remotely via PowerShell and sets it as the desktop background wallpaper.
  * **Workflow 3**: Initiates a 30-second emergency command-line system shutdown sequence.
* **Manual Light Controls**: Independent manual toggles for LEDs (LED 1, LED 2, LED 3) that remember state and restore cleanly after automated sequences finish.
* **Acoustic Feedback**: Buzzer notifications on workflow execution completion.

---

## 📦 Project Structure

```text
Lenardo-Controller/
├── android-app/        # Android Studio Jetpack Compose project source code[cite: 5]
├── arduino-firmware/   # Arduino C++ script and payload examples[cite: 5]
└── README.md           # Project documentation and setup instructions


🛠️ Hardware Requirements
Arduino Leonardo (ATmega32U4 chip required for native USB HID keyboard emulation).

HC-05 Bluetooth Module.

3x LEDs (with appropriate current-limiting resistors).

1x Active Buzzer.

Jumper wires & Breadboard
