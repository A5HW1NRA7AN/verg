variable "aws_region" {
  description = "The AWS region to deploy in"
  type        = string
  default     = "ap-northeast-1"
}

variable "instance_type" {
  description = "The EC2 instance type for Asterisk"
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "The name of the AWS Key Pair to generate"
  type        = string
  default     = "asterisk-generated-key"
}
