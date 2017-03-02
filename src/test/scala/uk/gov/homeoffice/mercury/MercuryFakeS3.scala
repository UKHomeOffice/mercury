package uk.gov.homeoffice.mercury

import org.specs2.mutable.SpecificationLike
import uk.gov.homeoffice.aws.s3.{Resource, S3}
import uk.gov.homeoffice.aws.s3.S3.ResourcesKey
import uk.gov.homeoffice.web.WebService

trait MercuryFakeS3 {
  this: SpecificationLike =>

  class MercuryAuthorized(override val s3: S3, override val webService: WebService with Authorization) extends Mercury(s3, webService) {
    /**
      * Due to an odd way the Fake S3 service works, we have to filter "groups" within Fake S3
      * @param resources Seq[Resource]
      * @return Map[ResourcesKey, Seq[Resource]
      */
    override def groupByTopDirectory(resources: Seq[Resource]): Map[ResourcesKey, Seq[Resource]] =
      super.groupByTopDirectory(resources).filterNot(_._1.contains("."))
  }
}