package io.ossim.omar.scdf.s3.source

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.messaging.Source
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.support.MessageBuilder
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener
import groovy.util.logging.Slf4j
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import groovy.json.JsonException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.AmazonServiceException
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.event.S3EventNotification
import org.springframework.beans.factory.annotation.Value
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.regex.Pattern

@SpringBootApplication
@EnableBinding(Source.class)
@Slf4j
class OmarS3SourceApplication {
  /**
   * Output channel
   */
	@Autowired
	@Output(Source.OUTPUT)
	private MessageChannel outputChannel

	@Autowired
	AmazonS3Client s3Client

	@Value('${s3.url:https://s3.amazonaws.com}')
	String s3Url

	@Value('${local.dir:/data}')
	String localDir

	@Value('${filter.file.type:nitf,nif}') 
	String filterFileType

	static void main(String[] args) {
		SpringApplication.run OmarS3SourceApplication, args
	}

	@MessageMapping('${sqs.queue}')
	@SqsListener(value = '${sqs.queue}', deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
	void receiveFromSqsQueue(final String sqsMessage)
	{
		log.debug("Message from queue: ${sqsMessage}")

		JsonBuilder parsedJsonData

		try
		{
			def parsedJson = new JsonSlurper().parseText(sqsMessage)
			log.debug("Parsed JSON:\n" + parsedJson.Message.toString())

			def recordsJson = new JsonSlurper().parseText(parsedJson.Message.toString())

			recordsJson.Records.each { record->
				String fileName = record.s3.object.key.toString()
				Pattern regexPattern = Pattern.compile("([^\\s]+(\\.(?i)(nitf|ntf))\$)")

				if(filterFileType != "")
				{
					String commaReplace = filterFileType.replaceAll(",","|")
					regexPattern = Pattern.compile("([^\\s]+(\\.(?i)(${commaReplace}))\$)")
				}

				log.debug("Regex Pattern: ${regexPattern}")

				if(fileName.matches("${regexPattern}"))
				{
					String bucketName = record.s3.bucket.name.toString()
					String fileFullPath = downloadFile(bucketName, fileName)

					log.debug("File Full Path: ${fileFullPath}")

					if(fileFullPath != "")
					{
						parsedJsonData = new JsonBuilder()
						parsedJsonData(
							filename: fileName,
							fileFullPath: fileFullPath
						)

						log.debug("Parsed data:\n" + parsedJsonData.toString())
						log.debug("Message sent: ${sendMessageOnOutputStream(parsedJsonData.toString())}")
					}
				}
    		}
		}
		catch (JsonException jsonEx)
		{
			log.warn("Message received is not in proper JSON format, skipping\n   Message body: ${message}")
		}
	}

	final String downloadFile(String bucketName, String fileName)
	{

		log.debug("Bucket: ${bucketName}, File Name: ${fileName}")

		String timeStamp = new SimpleDateFormat("yyyyMMddHH").format(Calendar.getInstance().getTime())
		String year = timeStamp.substring(0, 4)
		String month = timeStamp.substring(4,6)
		String day = timeStamp.substring(6,8)
		String hour = timeStamp.substring(8,10)
		String UUID = UUID.randomUUID().toString()
		String directories = localDir + "/" + year + "/" + month + "/" + day + "/" + hour + "/" + UUID + "/"
		File localFile
		ObjectMetadata object
		String filenameWithDirectory = ""

		try
		{
			if(fileName != "")
			{
				filenameWithDirectory = "${directories}${fileName}"
				localFile = new File(filenameWithDirectory)

				if (Files.notExists(Paths.get(directories)))
				{
					Files.createDirectories(Paths.get(directories))
				}

				// Download the file from S3
				object = s3Client.getObject(new GetObjectRequest(bucketName, fileName), localFile)
			}

		}
		catch (SdkClientException e)
		{
			log.error("Client error while attempting to download file: ${filename} from bucket: ${bucketName}", e)
		} catch (AmazonServiceException e)
		{
			log.error("Amazon S3 service error while attempting to download file: ${filename} from bucket: ${bucketName}", e)
		}

		return filenameWithDirectory
	}

	boolean sendMessageOnOutputStream(String message)
    {
        Message<String> messageToSend = MessageBuilder.withPayload(message)
        .setHeader(MessageHeaders.CONTENT_TYPE, '${spring.cloud.stream.bindings.output.content.type}')
        .build()

        outputChannel.send(messageToSend)
    }
}

