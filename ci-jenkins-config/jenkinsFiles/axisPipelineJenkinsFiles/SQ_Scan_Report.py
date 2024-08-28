#!/usr/bin/python
import csv
import os
import smtplib
import subprocess
import urllib

HTML_TABLE_TD = '<td >{0}</td>'
HTML_TABLE_TD_STRONG = '<td class="strong">{0}</td>'
SHORT_DATE = os.environ.get('short_date')
MAIL = os.environ['MAIL']
WORKSPACE = os.environ.get('WORKSPACE')
JOB_URL = os.environ['JOB_URL']
LAST_SUCCESS_VIOL_CNT_URL =JOB_URL +'/lastSuccessfulBuild/artifact/violationCnt.txt'
VIOL_CNT_FILE = WORKSPACE+'/violationCnt.txt'
DELTA_FILE = WORKSPACE+'/delta.txt'
REPORT_URL = JOB_URL +'/lastSuccessfulBuild/artifact/SQ_Scan_Report.html' 


guilty_repo_cnt = 0

row_index_repo = 0
row_index_issueIgnore = 1
row_index_skip = 2
row_index_coverageExc = 3
row_index_exclusions = 4
row_index_lombokExc = 5

row_index_issueIgnoreWC = 6
row_index_skipWC = 7
row_index_coverageExcWC = 8
row_index_exclusionsWC = 9

row_index_EPissueIgnore = 10
row_index_EPskip = 11
row_index_EPcoverageExc = 12
row_index_EPexclusions = 13
row_index_EPlombokExc = 14

row_index_EPissueIgnoreWC = 15
row_index_EPskipWC = 16
row_index_EPcoverageExcWC = 17
row_index_EPexclusionsWC = 18

row_index_suppressWarn = 19
row_index_atGenerated = 20
row_index_nonsonar = 21


def compare_2_files():
    with open(VIOL_CNT_FILE, 'r') as oFile:
        rowsViolCnt = csv.reader(oFile)
        for violCntRow in rowsViolCnt:
            lastSuccViolCntRow = getRowFromLastSuccessViolCnt(violCntRow[0], LAST_SUCCESS_VIOL_CNT_URL)
            rowArray = []
            if(lastSuccViolCntRow != ""):
                if(lastSuccViolCntRow == violCntRow):
                    rowArray = violCntRow
                else:
                    rowInDelta = []
                    rowInDelta.append(lastSuccViolCntRow[0])
                    for i in range(len(lastSuccViolCntRow)):
                        if(lastSuccViolCntRow[i].isdigit() and violCntRow[i].isdigit()):
                            elementDelta = int(violCntRow[i]) - int(lastSuccViolCntRow[i])
                            if(elementDelta != 0):
                                pozitiveSign = "+" if elementDelta > 0 else ""
                                diffColor="#cc0d04" if elementDelta > 0 else "#2bb804"
                                formatElementDiff="{0}{1}".format(pozitiveSign, elementDelta)
                                colourFormatDiff = '<span style="font-size:90%;color:{0}">({1})</span>'.format(diffColor,formatElementDiff)
                                rowInDelta.append("{0} {1}".format(violCntRow[i], colourFormatDiff))
                            else:
                                rowInDelta.append(violCntRow[i])

                    rowArray = rowInDelta
            else:
                #print "New row detected in violationCnt.txt"
                newRow = []
                newRow.append(violCntRow[0])
                for nElement in violCntRow:
                    if(nElement.isdigit()):
                        if(int(nElement) != 0):
                            colourFormatDiff = '<span style="font-size:85%;color:#cc0d04">(+'+nElement+')</span>'
                            newRow.append("{0} {1}".format(nElement, colourFormatDiff))
                        else:
                            newRow.append(nElement)
                rowArray = newRow
            if(all(value =='0' for value in rowArray[1:])):
                print "All elements are zeros. Row is ignored"
            else:
                global guilty_repo_cnt
                guilty_repo_cnt += 1
                with open(DELTA_FILE, 'a+') as diffFile:
                    diffFile.write("{0}\r\n".format(','.join(rowArray)))

def getRowFromLastSuccessViolCnt(element, fileName):
    matchedLine = ""
    f = urllib.urlopen(fileName)
    lastSuccessFile = f.read()
    lastSuccessFile = lastSuccessFile.split("\n")
    lastSuccLines = (line for line in csv.reader(lastSuccessFile) if line) # Non-blank lines
    for lastSuccLine in lastSuccLines:
        if(lastSuccLine[0] == element):
            matchedLine = lastSuccLine
            break
    return matchedLine



def csv_to_html_table():
    with open(DELTA_FILE) as file:
        file_content = file.readlines()
        file_content.sort()
    #reading file content into list
    rows = [x.strip() for x in file_content]
    table = "<tbody id='breakdownByRepoBody'>"
    table+= "".join(["<td>"+cell+"</td>" for cell in rows[0].split(",")])
    rows=rows[1:]
    #Converting csv to html row by row
    for row in rows:
        table+= "<tr>" + "".join(["<td>"+cell+"</td>" for cell in row.split(",")]) + "</tr>" + "\n"
    table+="</tbody><br>"
    return table

def number_of_violations(thingtocount):
    total_issueIgnore = 0
    total_skip = 0
    total_coverageExc = 0
    total_exclusions = 0
    total_lombokExc = 0

    total_issueIgnoreWC = 0
    total_skipWC = 0
    total_coverageExcWC = 0
    total_exclusionsWC = 0
    total_lombokExcWC = 0

    total_EPissueIgnore = 0
    total_EPskip = 0
    total_EPcoverageExc = 0
    total_EPexclusions = 0
    total_EPlombokExc = 0

    total_EPissueIgnoreWC = 0
    total_EPskipWC = 0
    total_EPcoverageExcWC = 0
    total_EPexclusionsWC = 0
    total_EPlombokExcWC = 0

    total_suppressWarn = 0
    total_atGenerated = 0
    total_nonsonar = 0

    total_violations = 0
    all_annotations = 0
    all_exclusions = 0
    all_wildCards = 0
    all_EPexclusions = 0
    all_EPwildCards = 0


    with open(VIOL_CNT_FILE) as file:
        csv_reader = csv.reader(file)
        for row in csv_reader:
            issueIgnore = int(row[row_index_issueIgnore])
            skip = int(row[row_index_skip])
            coverageExc = int(row[row_index_coverageExc])
            exclusions = int(row[row_index_exclusions])
            lombokExc = int(row[row_index_lombokExc])

            issueIgnoreWC = int(row[row_index_issueIgnoreWC])
            skipWC = int(row[row_index_skipWC])
            coverageExcWC = int(row[row_index_coverageExcWC])
            exclusionsWC = int(row[row_index_exclusionsWC])

            EPissueIgnore = int(row[row_index_EPissueIgnore])
            EPskip = int(row[row_index_EPskip])
            EPcoverageExc = int(row[row_index_EPcoverageExc])
            EPexclusions = int(row[row_index_EPexclusions])
            EPlombokExc = int(row[row_index_EPlombokExc])

            EPissueIgnoreWC = int(row[row_index_EPissueIgnoreWC])
            EPskipWC = int(row[row_index_EPskipWC])
            EPcoverageExcWC = int(row[row_index_EPcoverageExcWC])
            EPexclusionsWC = int(row[row_index_EPexclusionsWC])

            suppressWarn = int(row[row_index_suppressWarn])
            atGenerated = int(row[row_index_atGenerated])
            nonsonar = int(row[row_index_nonsonar])

            #pom scan
            total_issueIgnore += issueIgnore
            total_skip += skip
            total_coverageExc += coverageExc
            total_exclusions += exclusions
            total_lombokExc += lombokExc
            #pom scan - wildcards
            total_issueIgnoreWC += issueIgnoreWC
            total_skipWC += skipWC
            total_coverageExcWC += coverageExcWC
            total_exclusionsWC += exclusionsWC
            #Effective pom scan
            total_EPissueIgnore += EPissueIgnore
            total_EPskip += EPskip
            total_EPcoverageExc += EPcoverageExc
            total_EPexclusions += EPexclusions
            total_EPlombokExc += EPlombokExc
            #Effective Pom scan - Wild Cards
            total_EPissueIgnoreWC += EPissueIgnoreWC
            total_EPskipWC += EPskipWC
            total_EPcoverageExcWC += EPcoverageExcWC
            total_EPexclusionsWC += EPexclusionsWC
            #annotations - source code
            total_suppressWarn += suppressWarn
            total_atGenerated += atGenerated
            total_nonsonar += nonsonar

            total_violations = total_issueIgnore + total_skip + total_coverageExc + total_exclusions + total_lombokExc + total_suppressWarn + total_atGenerated + total_nonsonar
            all_exclusions = total_issueIgnore + total_skip + total_coverageExc + total_exclusions + total_lombokExc
            all_annotations =total_suppressWarn + total_atGenerated + total_nonsonar
            all_EPexclusions =total_EPissueIgnore + total_EPskip + total_EPcoverageExc + total_EPexclusions + total_EPlombokExc
            all_wildCards =total_issueIgnoreWC + total_skipWC + total_coverageExcWC + total_exclusionsWC
            all_EPwildCards =total_EPissueIgnoreWC + total_EPskipWC + total_EPcoverageExcWC + total_EPexclusionsWC

        if(thingtocount == "total_issueIgnore"):
            return str(total_issueIgnore)
        if(thingtocount == "total_skip"):
            return str(total_skip)
        if(thingtocount == "total_coverageExc"):
            return str(total_coverageExc)
        if(thingtocount == "total_exclusions"):
            return str(total_exclusions)
        if(thingtocount == "total_lombokExc"):
            return str(total_lombokExc)

        if(thingtocount == "total_issueIgnoreWC"):
            return str(total_issueIgnoreWC)
        if(thingtocount == "total_skipWC"):
            return str(total_skipWC)
        if(thingtocount == "total_coverageExcWC"):
            return str(total_coverageExcWC)
        if(thingtocount == "total_exclusionsWC"):
            return str(total_exclusionsWC)

        if(thingtocount == "total_EPissueIgnore"):
            return str(total_EPissueIgnore)
        if(thingtocount == "total_EPskip"):
            return str(total_EPskip)
        if(thingtocount == "total_EPcoverageExc"):
            return str(total_EPcoverageExc)
        if(thingtocount == "total_EPexclusions"):
            return str(total_EPexclusions)
        if(thingtocount == "total_EPlombokExc"):
            return str(total_EPlombokExc)

        if(thingtocount == "total_EPissueIgnoreWC"):
            return str(total_EPissueIgnoreWC)
        if(thingtocount == "total_EPskipWC"):
            return str(total_EPskipWC)
        if(thingtocount == "total_EPcoverageExcWC"):
            return str(total_EPcoverageExcWC)
        if(thingtocount == "total_EPexclusionsWC"):
            return str(total_EPexclusionsWC)

        if(thingtocount == "total_suppressWarn"):
            return str(total_suppressWarn)
        if(thingtocount == "total_atGenerated"):
            return str(total_atGenerated)
        if(thingtocount == "total_nonsonar"):
            return str(total_nonsonar)

        if(thingtocount == "all_exclusions"):
            return str(all_exclusions)
        if(thingtocount == "all_EPexclusions"):
            return str(all_EPexclusions)
        if(thingtocount == "all_wildCards"):
            return str(all_wildCards)
        if(thingtocount == "all_EPwildCards"):
            return str(all_EPwildCards)
        if(thingtocount == "all_annotations"):
            return str(all_annotations)

        return str(total_violations)


def number_of_repos():
    num_lines = sum(1 for line in open(VIOL_CNT_FILE))
    return str(num_lines)


def generate_report():
    html_str ='''
    <!doctype html>
    <html>
    <head>
        <title>Sonar Exclusion Report - Delta</title>
        <meta charset="utf-8" />
        <link rel="stylesheet" type="text/css" href="simplegrid.css" />

        <style>
            body {background:#eee;margin:0px; padding:0px; font-family: Arial,sans-serif;color: #333; font-size:90% }
            h1 {font-size: 1.4em;}
            h3 {font-size: 1em; font-weight:normal}
            h3, p {margin:0px; padding:0px;}
            .backgr {background: #fff;}
            .backgrBlue {background: rgb(6,68,150); color:#fff}
            .headerRow{background-color:rgb(6,68,150); padding: 10px;color:#fff}
            .pd-b-0{padding: 10px 10px 0px 10px;}
            .pd-t-0{padding: 0px 10px 10px 10px;}
            .pd-t-10{padding-top:10px}
            .pd-10{padding: 10px;}
            .mg-t-10{margin-top:10px;	}
            .strong {font-weight:bold}
            .small {font-size: 0.7em;}
            table {border-collapse: collapse; width: 100%;}
            td, th {border: 1px solid #dddddd;text-align: left; padding: 8px;}
            #summaryTbl td, #summaryViolations td {border:0;}
            #breakdownByRepo th { vertical-align:bottom; background: white; position: sticky; top: 0; box-shadow: 0 2px 2px -1px rgba(0, 0, 0, 0.4);}
            #breakdownByRepoBody tr:hover, #breakdownByRepoBody tr:nth-child(odd):hover {background-color:rgb(6,68,150,0.1); }
            #breakdownByRepoBody tr:nth-child(odd) { background-color:rgba(6,68,150,0.03)}
            #breakdownByRepoBody td:nth-child(1), #breakdownByRepoBody td:nth-child(6), #breakdownByRepoBody td:nth-child(10), #breakdownByRepoBody td:nth-child(15), #breakdownByRepoBody td:nth-child(19){ border-right: 1px solid #696969;}
            .verticalText span { writing-mode: vertical-lr; -webkit-writing-mode: vertical-lr; -ms-writing-mode: vertical-lr;transform: rotate(180deg); font-size:97%; height:200px; }
            .verticalCenter {vertical-align: middle !important; text-align:center !important;}
            .border_r {border-right: 1px solid #aaaaaa;}
        </style>
    </head>
    <body>
        <br>
        <div class="grid grid-pad">
            <div class="col-1-1">
            <div class="col-1-1 backgrBlue">
                <div class="headerRow pd-b-0">
                    <h1>Sonar Exclusion Report - Delta</h1>
                </div>
                    <div class="col-1-4 backgrBlue">
                       <div  class="pd-10">
                            <table class="summaryTbl">
                                <tr>
                                    <td>Date of the scan</td>
                                    '''
    with open("SQ_Scan_Report.html","w") as html_file:
        html_file.write(html_str)
        html_file.write(HTML_TABLE_TD_STRONG.format(SHORT_DATE))
        html_file.write("""         </tr>
                            </table>
                            <br>
                        </div>
                    </div>

                    <div class="col-1-4 backgrBlue">
                       <div  class="pd-10">
                            <table class="summaryTbl">
                                <tr>
                                    <td>Number of Repos scanned</td>
                                    """)
        html_file.write(HTML_TABLE_TD_STRONG.format(number_of_repos()))
        html_file.write("""         </tr>
                            </table>
                            <br>
                        </div>
                    </div>

                    <div class="col-1-4 backgrBlue">
                       <div  class="pd-10">
                            <table class="summaryTbl">
                                <tr>
                                    <td>Number of Repos with violations</td>
                                    """)
        html_file.write(HTML_TABLE_TD_STRONG.format(guilty_repo_cnt))
        html_file.write("""         </tr>
                            </table>
                            <br>
                        </div>
                    </div>

                    <div class="col-1-4 backgrBlue">
                       <div  class="pd-10">
                            <table class="summaryTbl">
                                <tr>
                                    <td>Number of violations found</td>
                                    """)
        html_file.write(HTML_TABLE_TD_STRONG.format(number_of_violations("total_violations")))
        html_file.write("""        </tr>
                            </table>
                            <br>
                        </div>
                    </div>
                    </div>
            </div>
        </div>

        <div class="grid grid-pad">
            <div class="col-5-12">
                <div class="col-1-1 backgr">
                    <div  class="pd-10 mg-t-10">
                        <div class="col-1-2">
                            <strong>Exclusions</strong>
                            <table>
                                    <tr>
                                        <td>issueIgnore</td>
                        """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_issueIgnore")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>skip</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_skip")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>coverageExc</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_coverageExc")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>exclusions</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_exclusions")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>lombokExc</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_lombokExc")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td class="strong">Total</td>
                                    """)
        html_file.write(HTML_TABLE_TD_STRONG.format(number_of_violations("all_exclusions")))
        html_file.write("""             </tr>
                            </table>
                            <p class="small pd-t-10">* Exclusions found in Pom files</P>
                            <br>
                        </div>

                        <div class="col-1-2 padding-l">
                            <strong>Exclusion WildCards</strong>
                            <table>
                                    <tr>
                                        <td>issueIgnoreWC</td>
                        """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_issueIgnoreWC")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>skipWC</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_skipWC")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>coverageExcWC</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_coverageExcWC")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>exclusionsWC</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_exclusionsWC")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td class="strong">Total</td>
                                    """)
        html_file.write(HTML_TABLE_TD_STRONG.format(number_of_violations("all_wildCards")))
        html_file.write("""             </tr>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-5-12">
                <div class="col-1-1 backgr">
                    <div  class="pd-10 mg-t-10">
                         <div class="col-1-2">
                            <strong>EP Exclusions </strong>
                            <table>
                                    <tr>
                                        <td>issueIgnore</td>
                        """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPissueIgnore")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>skip</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPskip")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>coverageExc</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPcoverageExc")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>exclusions</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPexclusions")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>lombokExc</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPlombokExc")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td class="strong">Total</td>
                                    """)
        html_file.write(HTML_TABLE_TD_STRONG.format(number_of_violations("all_EPexclusions")))
        html_file.write("""             </tr>
                            </table>

                            <p class="small pd-t-10">* Exclusions found in Effective Pom</P>
                            <br>
                        </div>

                        <div class="col-1-2">
                            <strong>EP Exclusion WildCards </strong>
                            <table>
                                    <tr>
                                        <td>issueIgnore</td>
                        """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPissueIgnoreWC")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>skip</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPskipWC")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>coverageExc</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPcoverageExcWC")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>exclusions</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_EPexclusionsWC")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td class="strong">Total</td>
                                    """)
        html_file.write(HTML_TABLE_TD_STRONG.format(number_of_violations("all_EPwildCards")))
        html_file.write("""             </tr>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-2-12">
                <div class="col-1-1 backgr">
                    <div  class="pd-10 mg-t-10">
                        <div class="col-1-1">
                            <strong>Annotations</strong>
                             <table>
                                    <tr>
                                        <td>suppressWarn</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_suppressWarn")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>@Generated</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_atGenerated")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td>nonsonar</td>
                                    """)
        html_file.write(HTML_TABLE_TD.format(number_of_violations("total_nonsonar")))
        html_file.write("""             </tr>
                                    <tr>
                                        <td class ="strong">Total</td>
                                    """)
        html_file.write(HTML_TABLE_TD_STRONG.format(number_of_violations("all_annotations")))
        html_file.write("""             </tr>
                                <tbody>
                            </table>
                            <br> <br><br> <br>
                            <p class="small">* Exclusions found in Source Code</P>
                            <br>
                        </div>
                    </div>
                </div>
            </div>
        </div>


        <div class="grid grid-pad">
            <div class="col-1-1">
            <div class="col-1-1 backgr">
                <div class="headerRow">
                    <h3>Breakdown per Repo</h3>
                </div>
                <div  class="pd-t-0">
                        """)
        html_table=csv_to_html_table()
        html_file.write("""<table id="breakdownByRepo">
                                <thead>
                                    <tr>
                                        <th colspan="1" class="border_r"> </th>
                                        <th colspan="5" class="border_r"> Exclusions </th>
                                        <th colspan="4" class="border_r"> WildCards </th>
                                        <th colspan="5" class="border_r"> Exclusions in EP</th>
                                        <th colspan="4" class="border_r"> WildCards in EP</th>
                                        <th colspan="3"> Annotations </th>
                                    </tr>
                                    <tr>
                                        <th class="verticalCenter border_r"><span>Repo </span></th>
                                        <th class="verticalText"><span>issueIgnore</span></th>
                                        <th class="verticalText"><span>skip</span></th>
                                        <th class="verticalText"><span>coverageExclusions</span></th>
                                        <th class="verticalText"><span>exclusions</span></th>
                                        <th class="verticalText border_r"><span>lombokExclusions </span></th>

                                        <th class="verticalText"><span>issueIgnore WC</span></th>
                                        <th class="verticalText"><span>skip WC</span></th>
                                        <th class="verticalText"><span>coverageExclusions WC</span></th>
                                        <th class="verticalText border_r"><span>exclusions WC</span></th>

                                        <th class="verticalText"><span>EPissueIgnore</span></th>
                                        <th class="verticalText"><span>EPskip</span></th>
                                        <th class="verticalText"><span>EPcoverageExclusions</span></th>
                                        <th class="verticalText"><span>EPexclusions</span></th>
                                        <th class="verticalText border_r"><span>EPlombokExclusions </span></th>

                                        <th class="verticalText"><span>EPissueIgnore WC</span></th>
                                        <th class="verticalText"><span>EPskip WC</span></th>
                                        <th class="verticalText"><span>EPcoverageExclusions WC</span></th>
                                        <th class="verticalText border_r"><span>EPexclusions WC</span></th>

                                        <th class="verticalText"><span>suppressWarn</span></th>
                                        <th class="verticalText"><span>@Generated</span></th>
                                        <th class="verticalText"><span>nonsonar</span></th>
                                    </tr>
                                </thead>
                    """ + html_table + """</tbody>""")
        html_file.write("""</table>
                <br>
                </div>
                </div>
            </div>
        </div>
        <br>
    </body></html>""")



def send_email():
    createEmail()
    commandEmail = """( echo To: """ + MAIL + """
        echo From: PDLDEAXISO@pdl.internal.ericsson.com
          echo "Content-Type: text/html; "
          echo Subject: "Sonar Exclusion Report - Delta"
         cat EmailToSend.txt ) | sendmail -t"""
    proc = subprocess.Popen(commandEmail,stdout=subprocess.PIPE,stderr=subprocess.PIPE,shell=True)
    (out, err) = proc.communicate()


def createEmail():
    emailContent = """<html>
    <head>
    <style>
        html, body { margin: 0 auto !important; padding: 0 !important; height: 100% !important; width: 100% !important; 
        background:#fff;font-family: Arial,sans-serif;color: #333;  }
        * {-ms-text-size-adjust: 100%;}
    </style>
    </head>
    <body width="100%" style="margin: 0; padding: 0 !important; mso-line-height-rule: exactly;">
        <div style="padding: 10px; height: 150px !important;text-align:center;background-color:rgb(6,68,150);">
        <br><br><br>
            <h2 style="color:#fff; font-weight:normal; padding-top: 60px !important; margin-bottom:5px">Sonar Exclusion Report - Delta </h2>
            <p style="color:#ccc; margin:0; font-size:15px"> generated every last Friday of the ENM sprint</p>
        <br>
        </div>
        <div style="padding:30px 40px 40px 40px !important;">
        <br>
            """
    with open("EmailToSend.txt", "w") as emailTxt:
        emailTxt.write(emailContent)
        
        emailTxt.write("""
            <br>
            <p style="padding-top:30px !important;"> Sonar Exclusion Report provides aggregated count of detected SonarQube exclusion tags in the project's pom and source code.
            <br>Tags found in <b><i>src/test</i></b> or <b><i>testsuite</i></b> are NOT counted.
            <p>Open newest <span style="font-weight: bold;">""")
        emailTxt.write(SHORT_DATE)
        emailTxt.write(""" </span>
            <a href=\"""" + REPORT_URL + """\">Sonar exclusions Report</a><p>
            <br><br><p style="font-size:80%;">For more details about the report, see <a href="https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/CICD/Sonar+Exclusion+Report+-+user+guide">Sonar Exclusion Report - user guide</a></p>
            <p>Thanks and Best Regards,
            <br>DE AXIS Ops</p>
        </div>
</body></html>
    """)



def main():
    compare_2_files()
    generate_report()
    send_email()

if __name__ == '__main__':
    main()