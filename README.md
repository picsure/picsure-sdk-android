# Picsure library for Android

<img src="assets/picsure.png" alt="Picsure">

![minSDK](https://img.shields.io/badge/SDK-15%2B-blue.svg)

Picsure generates insurance proposals based on image informations within seconds. This is the worldwide first AI in the insurance business based on image recognitions. We are providing our White-Label-API for insurance companies which allows them to create a new and full digital sales channel for their customers.

## Installation ⚙️ ##
#### [Gradle](https://gradle.org/docs/)
```
compile 'com.picsure:picsure_lib_android:1.1.0'

```
#### [Maven](https://maven.apache.org/guides/)
```
<dependency>
  <groupId>com.picsure</groupId>
  <artifactId>picsure_lib_android</artifactId>
  <version>1.1.0</version>
  <type>pom</type>
</dependency>

```

## Requirements
- Android SDK 15+


## Usage 🚀 ##


```java
//Init Picsure SDK with context and API_KEY
Picsure picsure = new Picsure(this, YOUR_API_KEY);

//Set Language if JSON response - default is "en" - ISO 4217
picsure.setLanguage("de");
```

Register the PicsureListener and override Methods to get the result callback
```java
 picsure.setOnEventListener(new PicsureListener() {
            @Override
            public void onResponse(JSONObject response) {

                Log.d("Picsure", response.toString());

            }

            @Override
            public void onError(String errorMessage) {

                Log.d("Picsure", errorMessage);

            }
        });
```

Upload a photo file to the Picsure API:
```java
//Send photo to Picsure-API
picsure.uploadPhoto(photoFile);
```

This is an example result JSON:
```json
{
  "object_recognition": [
    {
        "label": "Camera",
        "category": "camera",
        "subcategories": [],
        "price_indication": {
            "CHF": 560
        }
    },
    {
        "label": "Nikon D500",
        "category": "camera",
        "subcategories": [
            "nikon-d500",
            "nikon"
        ],
        "price_indication": {
            "CHF": 2393
        }
    },
    {
        "label": "Nikon D5",
        "category": "camera",
        "subcategories": [
            "nikon-d5",
            "nikon"
        ],
        "price_indication": {
            "CHF": 7639
        }
    },
    {
        "label": "DSLR Camera",
        "category": "camera",
        "subcategories": [
            "dslr-camera"
        ],
        "price_indication": {
            "CHF": 664
        }
    }
  ],
  "fraud_detection": {
        "exif_data_found": true,
        "location": {
            "lat": 50.717777777777776,
            "lng": 7.152222222222223
        },
        "create_date": "2017-11-29T10:15:25+00:00",
        "modify_date": "2017-11-29T10:15:25+00:00",
        "upload_date": "2018-01-19T14:29:33+00:00",
        "found_in_web": false,
        "days_photo_taken": 51,
        "days_photo_updated": 51,
        "is_modified": false
    }
}
```


## Authors

* **Florian Bischof** - florian@picsure.ai

## Licence

```
MIT License

Copyright (c) 2018 Picsure

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
