resource "aws_sns_topic" "billing_alert" {
  provider = aws.billing
  name     = "jobhunt-billing-alert"

  tags = {
    Project = "jobhunt"
  }
}

resource "aws_sns_topic_subscription" "billing_email" {
  provider  = aws.billing
  topic_arn = aws_sns_topic.billing_alert.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

resource "aws_cloudwatch_metric_alarm" "billing" {
  provider            = aws.billing
  alarm_name          = "jobhunt-monthly-billing-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "EstimatedCharges"
  namespace           = "AWS/Billing"
  period              = 21600
  statistic           = "Maximum"
  threshold           = 25
  alarm_description   = "Alert when estimated charges exceed $25"
  alarm_actions       = [aws_sns_topic.billing_alert.arn]

  dimensions = {
    Currency = "USD"
  }

  tags = {
    Project = "jobhunt"
  }
}
