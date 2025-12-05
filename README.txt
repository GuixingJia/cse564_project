Resilient Cyber-Physical Speed Enforcement System
Software Configuration & Execution Guide

1. Project Overview

This project implements a full roadside CPS (Cyber-Physical System) simulation for speed monitoring, license-plate recognition, evidence packaging, and backend upload.
All components are connected through a Spring Boot application and expose REST APIs for debugging and radar-sensor simulation.

This system simulates the entire speed-enforcement pipeline:

RadarData 
 → RadarDataCollector 
   → RadarSample
 → SpeedViolationController
   → (SpeedStatus + SpeedContext)
 → LEDDisplayController
   → LedCommand
 → EvidenceCaptureController
   → EvidenceCaptureResult
 → CameraDataCollector
 → ANPR Processor
 → EvidenceCollectorAndPackager
   → ViolationRecord
 → BackendUplinkController
   → UploadStatus

All components communicate using well-defined domain event structures.


2. Software Requirements
Component	Version
Java		JDK 17+
Spring Boot	3.x
Build Tool	Maven
OS			Windows / macOS / Linux


3. How to Configure & Run

You can simply use IEDA run src/main/resources/application.properties
Or use the method below

In application.properties, The road speed limit is currently set to 40 mph with a tolerance of 10%. If adjustments are needed, they can be made directly in this document.

3.1 Clone the Repository
Use IDEA
csd CSE564_Project

3.2 Build the Project

Ensure Maven is installed:
then use
mvn clean package

This produces:
target/cse564_project-0.0.1-SNAPSHOT.jar

3.3 Run the Application
mvn spring-boot:run

or:
java -jar target/cse564_project-0.0.1-SNAPSHOT.jar

3.4 Default Server
http://localhost:8080


4. API Controllers

The project contains two main controllers, each with different usage.

4.1 DebugSimulationController

This controller is used for demonstration and debugging, allowing you to test:
 - Full overspeed violation pipeline
 - Normal driving (non-overspeed)
 - Custom speed/distance combinations
 - Zone behavior (coarse-only, monitor-only, capture-window, leaving-window)
 - It simulates the CPS without requiring real radar or image input.

4.1.1 API List

For GET method below, I've set the value in url.
You can just use your browser.
Or use curl+"api url" in the terminal.

1)Simulate full overspeed violation

Method: GET 
http://localhost:8080/api/debug/simulate

Outputs LED status, capture activation, ANPR results, packaged violation record, and upload response.

2)Simulate normal driving

Method: GET
http://localhost:8080/api/debug/simulateNormal?speedMph=<value>

Example:
GET http://localhost:8080/api/debug/simulateNormal?speedMph=32

Shows LED message and confirms no violation is triggered.

3) Custom simulation of speed + distance

Method: GET
http://localhost:8080/api/debug/simulateCase?speedMph=<v>&distanceMiles=<d>

Example:
GET http://localhost:8080/api/debug/simulateCase?speedMph=48&distanceMiles=-0.01


Also shows zone classification:
Region	Meaning
 - OUT_OF_RANGE_BEFORE	Too far before entering zone
 - COARSE_ONLY	Rough monitoring, no evidence collection
 - MONITOR_ONLY	Speed monitoring, no capture
 - CAPTURE_WINDOW	Evidence capture enabled
 - LEAVING_STOP_CAPTURE	Capture stops
 - OUT_OF_RANGE_AFTER	Outside system range

Example curl
http://localhost:8080/api/debug/simulateCase?speedMph=48&distanceMiles=-0.01

4.2 RadarInputController

This controller simulates real radar input via POST requests.
 - It is the closest approximation to real CPS operation:
 - RadarData is POSTed
 - Pipeline processes the sample
 - Evidence is captured only inside the proper window
 - ViolationRecord uploaded to backend
 - This endpoint will be used in integration tests.

4.2.1 API List

POST radar sample

Method: POST
http://localhost:8080/api/radar/sample

Body: raw
Content-Type: application/json

{
  "distanceMiles": 0.02,
  "speedMph": 45
}

Sample response
{
	"input": {
		"distanceMiles": 0.02,
		"speedMph": 45
	},
	"radarSample": {
		"distanceMiles": 0.02,
		"speedMph": 45,
		"timestampMillis": 1764959437871,
		"targetId": 1
	},
	"speedStatus": {
		"speedMph": 45,
		"distanceMiles": 0.02,
		"overspeed": true
	},
	"overspeedContextPresent": true,
	"ledMessage": "OVERSPEED: 45.0 mph - SLOW DOWN",
	"captureActive": false,
	"stage": "EvidenceCaptureController",
	"reason": "Overspeed but outside capture window; ECC stopped capture."
}


Example curl
curl -X POST http://localhost:8080/api/radar/sample \
     -H "Content-Type: application/json" \
     -d '{"distanceMiles": -0.02, "speedMph": 50}'


5. Internal Unit Behavior Summary

Module Purpose
 - RadarDataCollector: Converts miles→meters, validates zone, tracks vehicle progression
 - SpeedViolationController: Determines overspeed + produces SpeedContext
 - LEDDisplayController: Builds LED message
 - EvidenceCaptureController: Decides capture/stop according to ±20m window
 - CameraDataCollector: Validates camera frame
 - AnprProcessor: Simulates plate recognition from predefined list
 - EvidenceCollectorAndPackager: Assembles full ViolationRecord
 - BackendUplinkController: Simulates upload, assigns backendRecordId

6. Typical End-to-End Flow Example

POST /api/radar/sample
   ↓
RadarDataCollector → RadarSample
   ↓
SpeedViolationController → SpeedStatus + SpeedContext
   ↓
LEDDisplayController → LedCommand
   ↓
EvidenceCaptureController → captureActive + SpeedContext
   ↓
CameraDataCollector → CameraData
   ↓
ANPR → PlateInfo
   ↓
EvidenceCollectorAndPackager → ViolationRecord
   ↓
BackendUplinkController → UploadStatus

7. Troubleshooting
It might happen when you are using port 8080.
“Port 8080 already in use”

Please Find the blocking process and kill it.