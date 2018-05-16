# Omar SCDF S3 Source
Omar SCDF S3 Source app monitors a given AWS SQS Queue and downloads the file specified in the SQS message.
## Assumptions ##
- The file to be downloaded is stored in a S3 Bucket.
- The S3 Bucket has a SNS Topic associated with it that sends a notification when a new file is added to the bucket.
- The SQS queue is registered to that SNS Topic.

## Messaging ##
The message sent by this app sends the following info:
- **filename** - The name of the file
- **fileFullPath** - The full path, which includes the filename, to where the file was downloaded to

## Options ##
Omar SCDF S3 Source app has the following properties which can be overwritten during deployment.
<dl>
  <dt>spring.cloud.stream.bindings.output.destination</dt>
  <dd>The message output desination. <strong>(String, default: files-extracted)</strong></dd>
</dl>
<dl>
  <dt>sqs.queue</dt>
  <dd>AWS SQS to listen to. <strong>(String, default: aws-sqs-queue)</strong></dd>
</dl>
<dl>
  <dt>local.dir</dt>
  <dd>Where the file is downloaded to. <strong>(String, default: /tmp)</strong></dd>
</dl>
<dl>
  <dt>filter.file.type</dt>
  <dd>Used to tell what type of files to download. <strong>(String, default: nitf,ntf)</strong></dd>
</dl>

## Amazon AWS common options ##
The Amazon S3 Source (as all other Amazon AWS applications) is based on the Spring Cloud AWS project as a foundation, and its auto-configuration classes are used automatically by Spring Boot. Consult their documentation regarding required and useful auto-configuration properties.

Some of them are about AWS credentials:

- cloud.aws.credentials.accessKey
- cloud.aws.credentials.secretKey
- cloud.aws.credentials.instanceProfile
- cloud.aws.credentials.profileName
- cloud.aws.credentials.profilePath

Other are for AWS Region definition:

- cloud.aws.region.auto
- cloud.aws.region.static

And for AWS Stack:

- cloud.aws.stack.auto
- cloud.aws.stack.name
