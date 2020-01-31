'use strict';
const functions = require('firebase-functions');
const mkdirp = require('mkdirp-promise');
const admin = require('firebase-admin');
//admin.initializeApp();
admin.initializeApp(functions.config().firebase);
const spawn = require('child-process-promise').spawn;
const path = require('path');
const os = require('os');
const fs = require('fs');

// Max height and width of the thumbnail in pixels.
const THUMB_MAX_HEIGHT = 200;
const THUMB_MAX_WIDTH = 200;
// Thumbnail prefix added to file names.
const THUMB_PREFIX = 'thumb_';
//Expiration year
var year = 2100;

/**
 * When an image is uploaded in the Storage bucket We generate a thumbnail automatically using
 * ImageMagick.
 * After the thumbnail has been generated and uploaded to Cloud Storage,
 * we write the public URL to the Firebase Realtime Database.
 */
exports.generateThumbnail = functions.storage.object().onFinalize(async (object) => {
  // File and directory paths.
  const filePath = object.name;
  const contentType = object.contentType; // This is the image MIME type
  const fileDir = path.dirname(filePath);
  const fileName = path.basename(filePath);
  const thumbFilePath = path.normalize(path.join(fileDir, `${THUMB_PREFIX}${fileName}`));
  const tempLocalFile = path.join(os.tmpdir(), filePath);
  const tempLocalDir = path.dirname(tempLocalFile);
  const tempLocalThumbFile = path.join(os.tmpdir(), thumbFilePath);

  // Exit if this is triggered on a file that is not an image.
  if (!contentType.startsWith('image/')) {
    return console.log('This is not an image.');
  }

  // Exit if the image is already a thumbnail.
  if (fileName.startsWith(THUMB_PREFIX)) {
    return console.log('Already a Thumbnail.');
  }

  // Cloud Storage files.
  const bucket = admin.storage().bucket(object.bucket);
  const file = bucket.file(filePath);
  const thumbFile = bucket.file(thumbFilePath);
  const metadata = {
    contentType: contentType,
  };
  
  // Create the temp directory where the storage file will be downloaded.
  await mkdirp(tempLocalDir)
  // Download file from bucket.
  await file.download({destination: tempLocalFile});
  console.log('The file has been downloaded to', tempLocalFile);
  // Generate a thumbnail using ImageMagick.
  await spawn('convert', [tempLocalFile, '-thumbnail', `${THUMB_MAX_WIDTH}x${THUMB_MAX_HEIGHT}>`, tempLocalThumbFile], {capture: ['stdout', 'stderr']});
  console.log('Thumbnail created at', tempLocalThumbFile);
  // Uploading the Thumbnail.
  try{
    await bucket.upload(tempLocalThumbFile, {destination: thumbFilePath, resumable: false ,metadata: metadata});
  }catch(error)
  {
    console.log('Error while try to ulpoad thumbnail, try again.');
    await bucket.upload(tempLocalThumbFile, {destination: thumbFilePath, resumable: false ,metadata: metadata});
  }
  
  console.log('Thumbnail uploaded to Storage at', thumbFilePath);
  // Once the image has been uploaded delete the local files to free up disk space.
  fs.unlinkSync(tempLocalFile);
  fs.unlinkSync(tempLocalThumbFile);
  // Get the Signed URLs for the thumbnail and original image.
  const config = {
    action: 'read',
    expires: '03-01-' + year,
  };
  const results = await Promise.all([
    thumbFile.getSignedUrl(config),
    file.getSignedUrl(config),
  ]);
  console.log('Got Signed URLs.');
  year++;//Increment year, to cause url be unique
  console.log('Expiration year: ' + year);
  const thumbResult = results[0];
  const originalResult = results[1];
  const thumbFileUrl = thumbResult[0];
  const fileUrl = originalResult[0];
  // Add the URLs to the Database
  const userPhone = fileName.split('.')[0];  
  await admin.firestore().collection('users').doc(userPhone).update({thumbUrl: thumbFileUrl, imageUrl: fileUrl});
  return console.log('Thumbnail URLs saved to database.');
});


exports.sendRecord = functions.storage.object().onFinalize(async (object) => {
  // File and directory paths.
  const filePath = object.name;
  const contentType = object.contentType; // This is the image MIME type
  const fileDir = path.dirname(filePath);
  const fileName = path.basename(filePath);

  // Exit if this is triggered on a file that is not an image.
  if (contentType.startsWith('image/')) {
    return console.log('This is not a record.');
  }

  // Cloud Storage files.
  const bucket = admin.storage().bucket(object.bucket);
  const file = bucket.file(filePath);
  
  // Get the Signed URLs for the thumbnail and original image.
  const config = {
    action: 'read',
    expires: '03-01-' + year,
  };
  const results = await Promise.all([
    file.getSignedUrl(config),
  ]);
  console.log('Got Signed URLs.');
  year++;//Increment year, to cause url be unique
  console.log('Expiration year: ' + year);
  const originalResult = results[0];
  const fileUrl = originalResult[0];

  console.log('Debug ' + fileDir);
  var dirName = fileDir.split('/')[1];
  var isGroup = dirName.includes("_");
  var topics = dirName.split("_");
  var fromName  = fileName.split("_")[1];
  var fromPhone  = fileName.split("_")[2];

  if(isGroup)
  {
    topics.shift();
  }

  topics.forEach(topic => {
    var message = {
      data: {
        fromName: fromName,
        fromPhone: fromPhone,
        recordUrl: fileUrl
      },
      topic: topic
    };
    
    // Send a message to devices subscribed to the provided topic.
    admin.messaging().send(message)
    .then((response) => {
      // Response is a message ID string.
      return console.log('Successfully sent message:', response);
    })
    .catch((error) => {
      return console.log('Error sending message:', error);
    });
  });

  return console.log('Function sendRecord finished successfully');
});

exports.onUserStatusChanged = functions.database.ref('/status/{userPhone}/isOnline') 
  .onUpdate((change, context) => {
    const status = change.after.val();
    const user = context.params.userPhone;
    admin.firestore().collection('users').doc(user).update({isOnline: status});
    return true;
  });

